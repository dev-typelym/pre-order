package com.app.preorder.orderservice.domain.order;

import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.domain.vo.OrderAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderDetailResponse {
    private Long orderId;
    private String status;
    private LocalDateTime orderDate;
    private BigDecimal totalPrice;
    private OrderAddress address;
    private List<OrderItemResponse> items;
}