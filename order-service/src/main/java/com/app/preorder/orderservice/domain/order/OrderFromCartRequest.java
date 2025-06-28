package com.app.preorder.orderservice.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderFromCartRequest {
    private List<OrderItemRequest> items;
}