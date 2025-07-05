package com.app.preorder.productservice.domain.entity;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.domain.vo.Period;
import jakarta.persistence.*;
import lombok.*;
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
    private Long id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private BigDecimal productPrice;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ENABLED;

    @Embedded
    private Period period;

    @OneToMany(mappedBy = "product")
    private List<Stock> stocks = new ArrayList<>();

    @Builder
    public Product(String productName, BigDecimal productPrice, String description, CategoryType category, Period period) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.description = description;
        this.category = category;
        this.period = period;
        this.status = ProductStatus.ENABLED; // 기본값
    }

    // === 도메인 메서드 ===
    public void updateProductName(String productName) {
        this.productName = productName;
    }

    public void updateProductPrice(BigDecimal productPrice) {
        this.productPrice = productPrice;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateCategory(CategoryType category) {
        this.category = category;
    }

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    public void updatePeriod(Period period) {
        this.period = period;
    }
}