    package com.app.preorder.productservice.entity;


    @Entity
    @Getter
    @ToString
    @Table(name = "tbl_stock")
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public class Stock {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        @EqualsAndHashCode.Include
        private Long id;

        private Long stockQuantity;

        @ManyToOne
        @JoinColumn(name = "product_id")
        private Product product;

        public Stock updateStockQuantity(Long stockQuantity){
            this.stockQuantity = stockQuantity;
            return this;
        }

    }
