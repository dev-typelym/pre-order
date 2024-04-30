package com.app.preorder.entity.cart;

import com.app.preorder.entity.member.Member;
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
public class Cart{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @OneToMany(fetch = FetchType.LAZY, mappedBy= "cart", cascade = CascadeType.ALL, orphanRemoval= true)
    private List<CartItem> cartItems = new ArrayList<CartItem>();

    @Builder
    public Cart(Long id , Member member){
        this.id = id;
        this.member = member;
    }
}