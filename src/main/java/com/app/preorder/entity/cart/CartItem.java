package com.app.preorder.entity.cart;

import com.app.preorder.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@ToString
@Table(name = "tbl_cart_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "CART_ID")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    private Long count;

    @Builder
    public CartItem(Cart cart, Product product, Long count) {
        this.cart = cart;
        this.product = product;
        this.count = count;
    }
}
