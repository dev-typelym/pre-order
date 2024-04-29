package com.app.preorder.entity.cart;

import com.app.preorder.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@ToString
@Table(name = "tbl_cart")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Cart{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    @Builder
    public Cart(Long id , Member member){
        this.id = id;
        this.member = member;
    }
}