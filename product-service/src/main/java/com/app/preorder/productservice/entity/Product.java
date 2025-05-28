package com.app.preorder.productservice.entity;

import com.app.preorder.type.CatergoryType;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
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

    @NotNull
    private String productName;

    @NotNull
    private BigDecimal productPrice;

    @NotNull
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private CatergoryType category;

    @OneToMany(mappedBy = "product")
    private List<Stock> stocks = new ArrayList<>();

}