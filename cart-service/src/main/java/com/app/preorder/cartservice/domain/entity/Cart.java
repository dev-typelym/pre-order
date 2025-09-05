package com.app.preorder.cartservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@Table(
        name = "tbl_cart",
        uniqueConstraints = @UniqueConstraint(name = "uq_cart_member", columnNames = "MEMBER_ID"),
        indexes = @Index(name = "idx_cart_member", columnList = "MEMBER_ID")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
@DynamicInsert
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
}
