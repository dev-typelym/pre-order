package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockDeductInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.orderservice.client.ProductServiceClient;
import com.app.preorder.orderservice.domain.order.OrderItemRequest;
import com.app.preorder.orderservice.domain.order.OrderResponse;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.OrderItem;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderScheduler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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

    // 단건 주문
    @Override
    @Transactional
    public Long orderSingleItem(Long memberId, Long productId, Long quantity) {
        ProductInternal product;
        StockInternal stock;

        try {
            product = productClient.getProductsByIds(List.of(productId)).get(0);
            stock = productClient.getStocksByIds(List.of(productId)).get(0);
        } catch (feign.FeignException ex) {
            throw new FeignException("상품 서비스 통신 오류", ex);
        }

        if (stock.getStockQuantity() < quantity) {
            throw new InsufficientStockException(
                    "상품 ID [" + productId + "]의 재고가 부족합니다. 요청 수량: " + quantity + ", 보유 재고: " + stock.getStockQuantity()
            );
        }

        OrderItem item = orderFactory.createOrderItem(product, quantity);
        Order order = orderFactory.createOrder(memberId, item);

        orderRepository.save(order);
        orderScheduler.scheduleAll(order.getId());

        return order.getId();
    }



    // 카트 다건 주문
    @Override
    @Transactional
    public Long orderFromCart(Long memberId, List<OrderItemRequest> items) {

        // [1] 주문할 상품 ID와 수량을 Map 형태로 변환
        Map<Long, Long> quantityMap = items.stream()
                .collect(Collectors.toMap(OrderItemRequest::getProductId, OrderItemRequest::getQuantity));

        // [2] 상품 ID 목록 추출
        List<Long> productIds = new ArrayList<>(quantityMap.keySet());

        List<ProductInternal> products;

        try {
            // [3] 상품 정보 조회 (FeignClient - product-service)
            products = productClient.getProductsByIds(productIds);
        } catch (FeignException e) {
            log.error("상품 서비스 조회 실패", e);
            throw new FeignException("상품 서비스 조회 실패", e);
        }

        // [4] 재고 차감 요청 (FeignClient - product-service)
        List<StockDeductInternal> deductList = items.stream()
                .map(i -> new StockDeductInternal(i.getProductId(), i.getQuantity()))
                .toList();

        try {
            productClient.deductStocks(deductList);
        } catch (FeignException e) {
            log.error("상품 서비스 재고 차감 실패", e);
            throw new FeignException("상품 서비스 재고 차감 실패", e);
        }

        // [5] 주문 엔티티 생성 (Factory 사용)
        Order order = orderFactory.createOrderFromCart(memberId, products, quantityMap);

        // [6] 주문 저장
        orderRepository.save(order);

        // [7] 주문 ID 반환
        return order.getId();
    }

    // 주문 목록
    @Override
    public Page<OrderResponse> getOrdersWithPaging(int page, Long memberId) {
        int pageSize = 10; // 고정값
        PageRequest pageable = PageRequest.of(page, pageSize);

        Page<Order> orders = orderRepository.findOrdersByMemberId(memberId, pageable);

        List<OrderResponse> responseList = orders.getContent().stream()
                .map(orderFactory::toOrderListDTO)
                .toList();

        return new PageImpl<>(responseList, pageable, orders.getTotalElements());
    }

    // 주문 상세보기
    @Override
    public OrderResponse getOrderItemsInOrder(Long orderId){
        Order orderItems = orderRepository.findOrderItemsByOrderId_queryDSL(orderId);
        OrderResponse orderItemListDTO = toOrderListDTO(orderItems);
        return orderItemListDTO;
    }


    // 주문 취소
    @Override
    public void orderCancel(Long orderId) {
        Order order = orderRepository.findOrderByOrderId_queryDSL(orderId);

        if (order.getStatus() != OrderStatus.ORDER_COMPLETE) {
            throw new IllegalStateException("Only completed orders can be canceled.");
        }

        order.updateOrderStatus(OrderStatus.ORDER_CANCEL);

        // 주문 항목에 대해 반복하여 재고 복원
        order.getOrderItems().forEach(item -> {
            Product product = item.getProduct();
            long quantity = item.getQuantity();

            // 해당 상품의 재고를 찾아서 주문 수량만큼 재고를 복원
            Stock stock = stockRepository.findStockByProductId_queryDSL(product.getId());
            if (stock != null) {
                long updatedQuantity = stock.getStockQuantity() + quantity;
                stock.updateStockQuantity(updatedQuantity);
            } else {
                throw new IllegalStateException("No stock record found for product ID: " + product.getId());
            }
        });
    }

    // 반품 신청
    @Override
    public void orderReturn(Long orderId) {
        Order order = orderRepository.findOrderByOrderId_queryDSL(orderId);

        if (order.getStatus() != OrderStatus.DELIVERY_COMPLETE) {
            throw new IllegalStateException("Only completed orders can be canceled.");
        }

        order.updateOrderStatus(OrderStatus.RETURNING);
    }


}
