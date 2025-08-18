package com.app.preorder.productservice.domain.entity;

import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.productservice.domain.entity.audit.AuditPeriod;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@ToString(exclude = "product")
@Table(name = "tbl_stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends AuditPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL/MariaDB면 이게 일반적
    private Long id;

    @Column(nullable = false)
    private Long stockQuantity;

    @Column(nullable = false)
    private Long reserved = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false) // 관계가 필수면 명시
    private Product product;

    @Builder
    public Stock(Long stockQuantity, Product product) {
        this.stockQuantity = stockQuantity;
        this.product = product;
    }

    // (표시용) DTO 만들 때만 편하게 쓰는 계산값
    @Transient
    public long getAvailable() {
        long qty = stockQuantity == null ? 0L : stockQuantity;
        long res = reserved == null ? 0L : reserved;
        long v = qty - res;
        return v < 0 ? 0 : v;
    }

    // (선택) 엔티티 save 경로 가드 — 벌크 JPQL엔 적용 안 됨
    @PrePersist @PreUpdate
    void validateBeforeSave() {
        if (stockQuantity == null) stockQuantity = 0L;
        if (reserved == null) reserved = 0L;
        if (reserved < 0) throw new IllegalStateException("reserved < 0");
        if (reserved > stockQuantity) {
            throw new IllegalStateException("reserved(" + reserved + ") > stockQuantity(" + stockQuantity + ")");
        }
    }

    // ↓↓↓ 필요 없으면 지워도 됨(어드민/테스트 전용) ↓↓↓
    public Stock updateStockQuantity(Long stockQuantity){
        this.stockQuantity = stockQuantity;
        return this;
    }

    public void decrease(Long quantity) {
        if (this.stockQuantity < quantity) {
            throw new InsufficientStockException("재고 부족: 현재 재고 [" + this.stockQuantity + "], 요청 차감 [" + quantity + "]");
        }
        this.stockQuantity -= quantity;
    }
}

