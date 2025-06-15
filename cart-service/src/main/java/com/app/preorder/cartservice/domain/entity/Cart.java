package com.app.preorder.cartservice.domain.entity;

import com.app.preorder.common.exception.custom.InvalidCartOperationException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@Table(name = "tbl_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@DynamicInsert
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    // MSA 외부 서비스 엔티티 참조 대신 ID만 저장
    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    @Builder
    public Cart(Long id, Long memberId) {
        this.id = id;
        this.memberId = memberId;
    }


    public void addOrUpdateItem(Long productId, Long count) {
        CartItem existing = this.cartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.updateCount(existing.getCount() + count);
        } else {
            CartItem item = CartItem.builder()
                    .cart(this)
                    .productId(productId)
                    .count(count)
                    .build();
            this.cartItems.add(item);
        }
    }

    public void decreaseItem(Long productId, Long count) {
        CartItem existing = this.cartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new InvalidCartOperationException("장바구니에 해당 상품이 없습니다."));

        long newCount = existing.getCount() - count;
        if (newCount <= 0) {
            this.cartItems.remove(existing);
        } else {
            existing.updateCount(newCount);
        }
    }

    public void deleteItemsByIds(List<Long> cartItemIds) {
        this.cartItems.removeIf(item -> cartItemIds.contains(item.getId()));
    }
}
