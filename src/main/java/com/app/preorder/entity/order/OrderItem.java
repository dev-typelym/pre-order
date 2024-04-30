package com.app.preorder.entity.order;

import com.app.preorder.entity.audit.Period;
import com.app.preorder.entity.cart.Cart;
import com.app.preorder.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long quantity;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    @Builder
    public OrderItem(Product product, Long quantity, LocalDateTime regDate) {
        this.product = product;
        this.quantity = quantity;
        this.regDate = regDate;
    }

}
