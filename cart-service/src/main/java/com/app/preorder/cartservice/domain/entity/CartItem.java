package com.app.preorder.cartservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@ToString
@Table(name = "tbl_cart_item")
@Getter
@DynamicUpdate
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CART_ID", nullable = false)
    private Cart cart;

    // MSA 외부 서비스 엔티티 참조 대신 ID만 저장
    @Column(name = "PRODUCT_ID", nullable = false)
    private Long productId;

    private Long count;

    public CartItem updateCount(Long count){
        this.count = count;
        return this;
    }

    @Builder
    public CartItem(Cart cart, Long productId, Long count) {
        this.cart = cart;
        this.productId = productId;
        this.count = count;
    }
}
