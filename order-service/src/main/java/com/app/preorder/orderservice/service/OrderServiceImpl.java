package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.orderservice.client.ProductServiceClient;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.OrderItem;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderScheduler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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
            product = productClient.getProductById(productId);
            stock = productClient.getStockById(productId);
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
    public Long addOrderFromCart(Long memberId, List<String> productIds, List<String> quantities) {
        Member member = memberRepository.findMemberById(memberId);
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalPrice = BigDecimal.ZERO;

        Order order = Order.builder()
                .orderDate(now)
                .member(member)
                .status(OrderStatus.ORDER_COMPLETE)
                .build();

        Map<String, Long> productIdToQuantityMap = new HashMap<>();

        // 상품별 수량을 맵에 저장
        for (int i = 0; i < productIds.size(); i++) {
            String productId = productIds.get(i);
            Long quantity = Long.parseLong(quantities.get(i));
            productIdToQuantityMap.put(productId, productIdToQuantityMap.getOrDefault(productId, 0L) + quantity);
        }

        for (Map.Entry<String, Long> entry : productIdToQuantityMap.entrySet()) {
            String productId = entry.getKey();
            Long productPerQuantity = entry.getValue();

            Product product = productRepository.findProductByProductId_queryDSL(Long.parseLong(productId));

            OrderItem orderItem = OrderItem.builder()
                    .quantity(productPerQuantity)
                    .product(product)
                    .regDate(now)
                    .build();

            order.addOrderItem(orderItem);
            totalPrice = totalPrice.add(orderItem.getProduct().getProductPrice().multiply(BigDecimal.valueOf(productPerQuantity)));

            // 재고 업데이트
            Stock stock = stockRepository.findStockByProductId_queryDSL(Long.parseLong(productId));
            stock.updateStockQuantity(stock.getStockQuantity() - productPerQuantity);

        }

        order.updateOrderPrice(totalPrice);
        orderRepository.save(order);
        return order.getId();
    }

    // 주문 목록
    @Override
    public Page<OrderListDTO> getOrderListWithPaging(int page, Long memberId) {
        Page<Order> orders = orderRepository.findAllOrder_queryDSL(PageRequest.of(page, 5), memberId);
        List<OrderListDTO> orderListDTOS = orders.getContent().stream()
                .map(this::toOrderListDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(orderListDTOS, orders.getPageable(), orders.getTotalElements());
    }

    // 주문 상세보기
    @Override
    public OrderListDTO getOrderItemsInOrder(Long orderId){
        Order orderItems = orderRepository.findOrderItemsByOrderId_queryDSL(orderId);
        OrderListDTO orderItemListDTO = toOrderListDTO(orderItems);
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
