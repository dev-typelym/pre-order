    package com.app.preorder.productservice.domain.entity;


    import com.app.preorder.common.exception.custom.InsufficientStockException;
    import com.app.preorder.productservice.domain.entity.audit.AuditPeriod;
    import jakarta.persistence.*;
    import lombok.*;

    @Entity
    @Getter
    @ToString
    @Table(name = "tbl_stock")
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class Stock extends AuditPeriod {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @EqualsAndHashCode.Include
        private Long id;

        @Column(nullable = false)
        private Long stockQuantity;

        @ManyToOne
        @JoinColumn(name = "product_id")
        private Product product;

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
