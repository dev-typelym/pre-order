package com.app.preorder.entity.like;

import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@ToString(exclude = {"product", "member"})
@Table(name = "tbl_product_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLike {
    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public ProductLike(Product product, Member member) {
        this.product = product;
        this.member = member;
    }
}
