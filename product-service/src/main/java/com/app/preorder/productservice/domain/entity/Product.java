package com.app.preorder.productservice.domain.entity;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.domain.entity.audit.AuditPeriod;
import com.app.preorder.productservice.domain.vo.SalesPeriod;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@Table(name = "tbl_product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends AuditPeriod {

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
    private SalesPeriod salesPeriod;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stock> stocks = new ArrayList<>();

    @Builder
    public Product(String productName, BigDecimal productPrice, String description, CategoryType category, SalesPeriod salesPeriod) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.description = description;
        this.category = category;
        this.salesPeriod = salesPeriod;
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

    public void updatePeriod(SalesPeriod salesPeriod) {
        this.salesPeriod = salesPeriod;
    }
}