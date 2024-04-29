package com.app.preorder.entity.product;

import com.app.preorder.entity.like.ProductLike;
import com.app.preorder.entity.member.Member;
import com.app.preorder.type.CatergoryType;
import jakarta.persistence.*;
import lombok.*;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.ws.soap.addressing.server.annotation.Address;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@Table(name = "tbl_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @NonNull
    private String productName;

    @NotNull
    private Long productPrice;

    @NotNull
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CatergoryType category;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "product")
    private List<Stock> stocks = new ArrayList<>();

}