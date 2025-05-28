package com.app.preorder.orderservice.domain.orderDTO;

import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import com.app.preorder.type.OrderStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class OrderListDTO {

    private Long id;
    private String orderTitle;
    private BigDecimal orderPrice;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private Member member;
    private List<OrderItem> orderItems = new ArrayList<>();
    private LocalDateTime regDate;
    private LocalDateTime updateDate;




}
