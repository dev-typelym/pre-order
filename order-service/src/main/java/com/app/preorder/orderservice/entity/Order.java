package com.app.preorder.orderservice.entity;

import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.domain.vo.OrderAddress;
import com.app.preorder.orderservice.entity.audit.Period;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@Table(name = "tbl_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@DynamicInsert
public class Order extends Period {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외부 노출용 주문번호
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(nullable = false)
    private Long memberId;  // 연관 엔티티 대신 memberId만 저장

    @Embedded
    private OrderAddress deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal orderPrice;

    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 연관관계 메서드
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public Order updateOrderStatus(OrderStatus status) {
        this.status = status;
        return this;
    }

    public void updateDeliveryAddress(OrderAddress newAddress) {
        this.deliveryAddress = newAddress;
    }

    public void updateOrderPrice(BigDecimal totalPrice) {
        this.orderPrice = totalPrice;
    }

    @Builder
    public Order(String orderNumber, Long memberId, OrderAddress deliveryAddress, OrderStatus status,
                 BigDecimal orderPrice, LocalDateTime orderDate) {
        this.orderNumber = orderNumber;
        this.memberId = memberId;
        this.deliveryAddress = deliveryAddress;
        this.status = status;
        this.orderPrice = orderPrice;
        this.orderDate = orderDate;
    }
}