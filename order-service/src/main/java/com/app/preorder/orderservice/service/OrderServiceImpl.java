// order-service/src/main/java/com/app/preorder/orderservice/service/OrderServiceImpl.java
package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.exception.custom.*;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.client.ProductServiceClient;
import com.app.preorder.orderservice.domain.order.*;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.idempotency.OrderStepIdempotency;
import com.app.preorder.orderservice.messaging.publisher.OrderCommandPublisher;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderQuartzScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

    // ▼ Invoker 제거, 조회용 Feign만 유지
    private final ProductServiceClient productClient;

    private final OrderRepository orderRepository;
    private final OrderFactory orderFactory;
    private final OrderQuartzScheduler orderQuartzScheduler;
    private final OrderTransactionalService orderTransactionalService;

    // ▼ Kafka 커맨드 퍼블리셔 사용
    private final OrderCommandPublisher orderCommandPublisher;

    private final OrderStepIdempotency idem;

    // 단건 주문 준비: 상품 조회(Feign) → 주문 저장 → 재고 예약 커맨드 발행(Kafka)
    @Override
    public Long prepareSingleOrder(Long memberId, Long productId, Long quantity) {
        List<ProductInternal> products;
        try {
            products = productClient.getProductsByIds(List.of(productId));
        } catch (Exception e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
        if (products.isEmpty()) throw new ProductNotFoundException("상품을 찾을 수 없습니다.");

        // 주문 먼저 저장(상태: PAYMENT_PREPARING 등)
        Long orderId = orderTransactionalService.saveOrderInTransaction(memberId, products.get(0), quantity);

        // 재고 예약은 비동기 커맨드로 전환
        List<StockRequestInternal> reserveList = List.of(new StockRequestInternal(productId, quantity));
        orderCommandPublisher.publishReserveCommand(orderId, reserveList);

        // 예약 성공/실패 처리는 StockCommandResult 컨슈머에서 멱등적으로 마무리
        return orderId;
    }

    // 장바구니 다건 주문 준비: 조회 → 주문 저장 → 예약 커맨드 발행
    @Override
    public Long prepareCartOrder(Long memberId, List<OrderItemRequest> items) {
        Map<Long, Long> quantityMap = items.stream()
                .collect(Collectors.toMap(OrderItemRequest::getProductId, OrderItemRequest::getQuantity));
        List<Long> productIds = new ArrayList<>(quantityMap.keySet());

        List<ProductInternal> products;
        try {
            products = productClient.getProductsByIds(productIds);
        } catch (Exception e) {
            throw new RuntimeException("상품 조회 실패", e);
        }
        if (products.isEmpty()) throw new ProductNotFoundException("상품을 찾을 수 없습니다.");

        Long orderId = orderTransactionalService.saveOrderFromCartInTransaction(memberId, products, quantityMap);

        List<StockRequestInternal> reserveList = items.stream()
                .map(i -> new StockRequestInternal(i.getProductId(), i.getQuantity()))
                .toList();
        orderCommandPublisher.publishReserveCommand(orderId, reserveList);

        return orderId;
    }

    // 결제 시도: 상태만 전이 (예약 결과는 비동기로 옴)
    @Override
    @Transactional
    public void attemptPayment(Long orderId, Long memberId) {
        Order order = findOrder(orderId);
        if (!order.getMemberId().equals(memberId)) throw new ForbiddenException("본인 주문만 결제 시도 가능합니다.");
        if (order.getStatus() != OrderStatus.PAYMENT_PREPARING) throw new InvalidOrderStatusException("결제 시도를 할 수 없는 상태입니다.");
        try {
            idem.begin(orderId, "ATTEMPT", null);
            orderTransactionalService.updateOrderStatusToProcessing(order);
        } catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
        }
    }

    // 결제 완료: 재고 커밋을 동기 호출 대신 커맨드로 발행 → 실제 완료는 결과 이벤트 컨슈머에서 처리
    @Override
    @Transactional
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
            idem.begin(orderId, "COMPLETE", null);
            orderCommandPublisher.publishCommitCommand(orderId, items);
        } catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
        } catch (RuntimeException e) {
            idem.undo(orderId, "COMPLETE", null);
            throw e;
        }
    }

    // 주문 목록 조회
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

    // 주문 상세 조회
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long memberId, Long orderId) {
        Order order = orderRepository.findOrderItemsById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다."));
        if (!order.getMemberId().equals(memberId)) throw new ForbiddenException("본인의 주문만 조회할 수 있습니다.");
        return orderFactory.toOrderDetailResponse(order);
    }

    // 주문 배송지 수정
    @Override
    public void updateOrderAddress(Long orderId, UpdateOrderAddressRequest request) {
        Order order = findOrder(orderId);
        if (!OrderStatus.ORDER_COMPLETE.equals(order.getStatus())) {
            throw new InvalidOrderStatusException("배송지 수정은 주문 완료 상태에서만 가능합니다.");
        }
        orderTransactionalService.updateOrderAddressInTransaction(order, request);
    }

    // 주문 취소: 바로 복원 커맨드 발행(동기 복원 제거)
    @Override
    @Transactional
    public void orderCancel(Long orderId) {
        Order order = findOrder(orderId);
        if (!OrderStatus.ORDER_COMPLETE.equals(order.getStatus())) {
            throw new InvalidOrderStatusException("완료된 주문만 취소할 수 있습니다.");
        }

        List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                .toList();

        try {
            idem.begin(orderId, "CANCEL", null);
            orderTransactionalService.cancelOrderInTransaction(order);
            // 동기 호출 대신 바로 커맨드 발행
            orderCommandPublisher.publishStockRestoreCommand(orderId, restoreItems);
        } catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
        } catch (RuntimeException e) {
            idem.undo(orderId, "CANCEL", null);
            throw e;
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
            orderQuartzScheduler.scheduleReturnProcess(orderId);
        } catch (Exception e) {
            throw new RuntimeException("반품 스케줄 등록 실패", e);
        }

        try {
            orderTransactionalService.updateOrderStatusToReturning(order);
        } catch (Exception e) {
            try { orderQuartzScheduler.cancelReturnProcess(orderId); }
            catch (Exception cancelEx) { log.error("보상 트랜잭션(스케줄 취소) 실패 - orderId: {}", orderId, cancelEx); }
            throw e;
        }
    }

    // 배송 상태를 배송중으로 변경
    @Override
    public void updateOrderStatusShipping(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.ORDER_COMPLETE) {
            orderTransactionalService.updateOrderStatusToShipping(order);
        }
    }

    // 배송 상태를 배송완료로 변경
    @Override
    public void updateOrderStatusDelivered(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.DELIVERYING) {
            orderTransactionalService.updateOrderStatusToDelivered(order);
        }
    }

    // 반품 불가 상태로 변경
    @Override
    public void updateOrderStatusNonReturnable(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.DELIVERY_COMPLETE) {
            orderTransactionalService.updateOrderStatusToNonReturnable(order);
        }
    }

    // 반품 처리(상태 전이 + 재고 복원 커맨드 발행)
    @Override
    @Transactional
    public void processReturn(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.DELIVERY_COMPLETE) {
            throw new InvalidOrderStatusException("배송 완료 상태의 주문만 반품 처리할 수 있습니다.");
        }

        List<StockRequestInternal> restoreItems = order.getOrderItems().stream()
                .map(item -> new StockRequestInternal(item.getProductId(), item.getProductQuantity()))
                .toList();

        try {
            idem.begin(orderId, "RETURN", null);
            orderTransactionalService.updateOrderStatusToReturnComplete(order);
            // 동기 복원 제거 → 커맨드 발행
            orderCommandPublisher.publishStockRestoreCommand(orderId, restoreItems);
        } catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
        } catch (RuntimeException e) {
            idem.undo(orderId, "RETURN", null);
            throw e;
        }
    }

    // 주문 조회 헬퍼
    private Order findOrder(Long orderId) {
        return orderRepository.findOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("주문을 찾을 수 없습니다. id=" + orderId));
    }
}
