package com.app.preorder.orderservice.entity;

import com.app.preorder.orderservice.entity.audit.Period;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@Table(name = "tbl_order_item")
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends Period {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long quantity;

    @Builder
    public OrderItem(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    // 연관관계 설정
    public void setOrder(Order order) {
        this.order = order;
    }
}
