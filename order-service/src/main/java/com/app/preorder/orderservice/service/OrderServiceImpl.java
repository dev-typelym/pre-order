package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.exception.custom.*;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.client.ProductServiceClient;
import com.app.preorder.orderservice.domain.order.OrderDetailResponse;
import com.app.preorder.orderservice.domain.order.OrderItemRequest;
import com.app.preorder.orderservice.domain.order.OrderResponse;
import com.app.preorder.orderservice.domain.order.UpdateOrderAddressRequest;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.messaging.publisher.OrderEventPublisher;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    private final ProductServiceClient productClient;
    private final OrderRepository orderRepository;
    private final OrderFactory orderFactory;
    private final OrderScheduler orderScheduler;
    private final OrderTransactionalService orderTransactionalService;
    private final OrderEventPublisher orderEventPublisher; // ✅ 퍼블리셔 주입

    // 단건 주문 준비
    @Override
    public Long prepareSingleOrder(Long memberId, Long productId, Long quantity) {
        List<ProductInternal> products;
        try {
            products = productClient.getProductsByIds(List.of(productId));
        } catch (FeignException e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
        if (products.isEmpty()) throw new ProductNotFoundException("상품을 찾을 수 없습니다.");

        // 재고 예약
        List<StockRequestInternal> reserveList = List.of(new StockRequestInternal(productId, quantity));
        try {
            productClient.reserveStocks(reserveList);
        } catch (FeignException e) {
            throw new InsufficientStockException("재고 예약 실패");
        }

        return orderTransactionalService.saveOrderInTransaction(memberId, products.get(0), quantity);
    }

    // 카트 다건 주문 준비
    @Override
    public Long prepareCartOrder(Long memberId, List<OrderItemRequest> items) {
        Map<Long, Long> quantityMap = items.stream()
                .collect(Collectors.toMap(OrderItemRequest::getProductId, OrderItemRequest::getQuantity));
        List<Long> productIds = new ArrayList<>(quantityMap.keySet());

        List<ProductInternal> products;
        try {
            products = productClient.getProductsByIds(productIds);
        } catch (FeignException e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
        if (products.isEmpty()) throw new ProductNotFoundException("상품을 찾을 수 없습니다.");

        List<StockRequestInternal> reserveList = items.stream()
                .map(i -> new StockRequestInternal(i.getProductId(), i.getQuantity()))
                .toList();

        try {
            productClient.reserveStocks(reserveList);
        } catch (FeignException e) {
            throw new InsufficientStockException("재고 예약 실패(장바구니)");
        }

        return orderTransactionalService.saveOrderFromCartInTransaction(memberId, products, quantityMap);
    }

    // 결제 시도
    @Override
    public void attemptPayment(Long orderId, Long memberId) {
        Order order = findOrder(orderId);
        if (!order.getMemberId().equals(memberId)) throw new ForbiddenException("본인 주문만 결제 시도 가능합니다.");
        if (order.getStatus() != OrderStatus.PAYMENT_PREPARING) throw new InvalidOrderStatusException("결제 시도를 할 수 없는 상태입니다.");

        orderTransactionalService.updateOrderStatusToProcessing(order);
    }

    // 결제 완료
    @Override
    public void completePayment(Long orderId, Long memberId) {
        Order order = findOrder(orderId);
        if (!order.getMemberId().equals(memberId)) throw new ForbiddenException("본인 주문만 결제 완료 가능합니다.");
        if (order.getStatus() != OrderStatus.PAYMENT_PREPARING
                && order.getStatus() != OrderStatus.PAYMENT_PROCESSING) {
            throw new InvalidOrderStatusException("결제를 완료할 수 없는 상태입니다.");
        }

        List<StockRequestInternal> items = order.getOrderItems().stream()
                .map(i -> new StockRequestInternal(i.getProductId(), i.getProductQuantity()))
                .toList();

        try {
            productClient.commitStocks(items);
        } catch (FeignException e) {
            try { productClient.unreserveStocks(items); } catch (FeignException ignore) {}
            throw new RuntimeException("결제 완료 실패(재고 커밋 불가)", e);
        }

        orderTransactionalService.completeOrder(order);
    }

    // 주문 목록
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersWithPaging(int page, int size, Long memberId) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findOrdersByMemberId(memberId, pageable);
        List<OrderResponse> responseList = orders.getContent().stream()
                .map(orderFactory::toOrderResponse)
                .toList();
        return new PageImpl<>(responseList, pageable, orders.getTotalElements());
    }

    // 주문 상세
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
        Order order = orderRepository.findOrderItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));
        if (!order.getMemberId().equals(memberId)) throw new ForbiddenException("본인의 주문만 조회할 수 있습니다.");
        return orderFactory.toOrderDetailResponse(order);
    }

    @Override
    public void updateOrderAddress(Long orderId, UpdateOrderAddressRequest request) {
        Order order = findOrder(orderId);
        if (!OrderStatus.ORDER_COMPLETE.equals(order.getStatus())) {
            throw new InvalidOrderStatusException("배송지 수정은 주문 완료 상태에서만 가능합니다.");
        }
        orderTransactionalService.updateOrderAddressInTransaction(order, request);
    }

    // 주문 취소
    @Override
    public void orderCancel(Long orderId) {
        Order order = findOrder(orderId);
        if (!OrderStatus.ORDER_COMPLETE.equals(order.getStatus())) {
            throw new InvalidOrderStatusException("완료된 주문만 취소할 수 있습니다.");
        }

        // 1) 상태 전이(취소)
        orderTransactionalService.cancelOrderInTransaction(order);

        // 2) 재고 복원(동기 시도 → 실패 시 카프카로 비동기 요청)
        List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                .toList();
        try {
            productClient.restoreStocks(restoreItems);
        } catch (FeignException e) {
            log.warn("동기 재고 복원 실패 → Kafka로 복원 요청 전환. orderId={}", orderId, e);
            orderEventPublisher.publishStockRestoreRequest(orderId, restoreItems);
        }
    }

    // 반품 신청
    @Override
    public void orderReturn(Long orderId) {
        Order order = findOrder(orderId);
        if (!OrderStatus.DELIVERY_COMPLETE.equals(order.getStatus())) {
            throw new InvalidOrderStatusException("배송 완료된 주문만 반품 신청할 수 있습니다.");
        }

        try {
            orderScheduler.scheduleReturnProcess(orderId);
        } catch (Exception e) {
            throw new RuntimeException("반품 스케줄 등록 실패", e);
        }

        try {
            orderTransactionalService.updateOrderStatusToReturning(order);
        } catch (Exception e) {
            try { orderScheduler.cancelReturnProcess(orderId); }
            catch (Exception cancelEx) { log.error("보상 트랜잭션(스케줄 취소) 실패 - orderId: {}", orderId, cancelEx); }
            throw e;
        }
    }

    // 배송 중
    @Override
    public void updateOrderStatusShipping(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.ORDER_COMPLETE) {
            // 트랜잭션 서비스로 위임 (더티체킹 보장)
            orderTransactionalService.updateOrderStatusToShipping(order);
        }
    }

    // 배송 완료
    @Override
    public void updateOrderStatusDelivered(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.DELIVERYING) {
            orderTransactionalService.updateOrderStatusToDelivered(order);
        }
    }

    // 반품 불가
    @Override
    public void updateOrderStatusNonReturnable(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.DELIVERY_COMPLETE) {
            orderTransactionalService.updateOrderStatusToNonReturnable(order);
        }
    }

    // 반품 처리 (RETURN_COMPLETE + 복원)
    @Override
    public void processReturn(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.DELIVERY_COMPLETE) {
            throw new InvalidOrderStatusException("배송 완료 상태의 주문만 반품 처리할 수 있습니다.");
        }

        // 1) 상태 전이 먼저
        orderTransactionalService.updateOrderStatusToReturnComplete(order);

        // 2) 재고 복원 (실패 시 카프카 요청)
        List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                .toList();
        try {
            productClient.restoreStocks(restoreItems);
        } catch (FeignException e) {
            log.warn("반품 재고 복원 동기 실패 → Kafka 복원 요청으로 전환. orderId={}", orderId, e);
            orderEventPublisher.publishStockRestoreRequest(orderId, restoreItems);
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
    }
}
