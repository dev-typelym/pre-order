package com.app.preorder.orderservice.factory;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
                    .price(product.getPrice())
                    .quantity(quantity)
                    .build();

            order.addOrderItem(item);
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.updateOrderPrice(totalPrice);
        return order;
    }
}
