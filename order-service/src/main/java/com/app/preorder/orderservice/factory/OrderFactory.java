package com.app.preorder.orderservice.factory;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.OrderItem;

import java.time.LocalDateTime;

public class OrderFactory {

    public OrderItem createOrderItem(ProductInternal product, Long quantity) {
        return OrderItem.builder()
                .productId(product.getId())
                .price(product.getPrice())
                .quantity(quantity)
                .build();
    }

    public Order createOrder(Long memberId, OrderItem item) {
        Order order = Order.builder()
                .memberId(memberId)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.ORDER_COMPLETE)
                .orderPrice(item.getPrice())
                .build();
        order.addOrderItem(item);
        return order;
    }
}
