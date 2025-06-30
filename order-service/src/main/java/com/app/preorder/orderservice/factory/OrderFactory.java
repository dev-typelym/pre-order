package com.app.preorder.orderservice.factory;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.domain.order.OrderDetailResponse;
import com.app.preorder.orderservice.domain.order.OrderItemResponse;
import com.app.preorder.orderservice.domain.order.OrderResponse;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class OrderFactory {

    public OrderItem createOrderItem(ProductInternal product, Long quantity) {
        return OrderItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(product.getPrice())
                .productQuantity(quantity)
                .build();
    }

    public Order createOrder(Long memberId, OrderItem item) {
        Order order = Order.builder()
                .memberId(memberId)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.ORDER_COMPLETE)
                .orderPrice(item.getProductPrice())
                .build();
        order.addOrderItem(item);
        return order;
    }

    public Order createOrderFromCart(Long memberId, List<ProductInternal> products, Map<Long, Long> quantityMap) {
        Order order = Order.builder()
                .memberId(memberId)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.ORDER_COMPLETE)
                .build();

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (ProductInternal product : products) {
            Long quantity = quantityMap.get(product.getId());

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productPrice(product.getPrice())
                    .productQuantity(quantity)
                    .build();

            order.addOrderItem(item);
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.updateOrderPrice(totalPrice);
        return order;
    }

    public OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getOrderPrice())
                .createdAt(order.getRegisterDate())
                .build();
    }

    public OrderDetailResponse toOrderDetailResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductPrice(),
                        item.getProductQuantity(),
                        item.getProductPrice().multiply(BigDecimal.valueOf(item.getProductQuantity()))
                ))
                .toList();

        BigDecimal totalPrice = itemResponses.stream()
                .map(OrderItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderDetailResponse(
                order.getId(),
                order.getStatus().name(),              // ✅ Enum → String 변환
                order.getRegisterDate(),
                totalPrice,
                order.getDeliveryAddress(),
                itemResponses
        );
    }
}
