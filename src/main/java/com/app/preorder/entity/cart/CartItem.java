package com.app.preorder.entity.cart;

import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.product.Product;
import com.app.preorder.type.Role;
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

    @ManyToOne
    @JoinColumn(name = "CART_ID")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    private Long count;

    public CartItem updateCount(Long count){
        this.count = count;
        return this;
    }

    @Builder
    public CartItem(Cart cart, Product product, Long count) {
        this.cart = cart;
        this.product = product;
        this.count = count;
    }

}
