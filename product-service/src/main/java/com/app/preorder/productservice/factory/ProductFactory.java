package com.app.preorder.productservice.factory;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.domain.entity.Stock;
import com.app.preorder.productservice.domain.vo.SalesPeriod;
import com.app.preorder.productservice.dto.product.ProductCreateRequest;
import com.app.preorder.productservice.dto.product.ProductResponse;
import com.app.preorder.productservice.dto.product.ProductUpdateRequest;
import com.app.preorder.productservice.dto.stock.ProductStockResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductFactory {

    //  상품 생성
    public Product createFrom(ProductCreateRequest request) {
        SalesPeriod salesPeriod = SalesPeriod.builder()
                .startAt(request.getSaleStartAt())
                .endAt(request.getSaleEndAt())
                .build();

        return Product.builder()
                .productName(request.getProductName())
                .productPrice(request.getProductPrice())
                .description(request.getDescription())
                .category(request.getCategory())
                .salesPeriod(salesPeriod)
                .build();
    }

    //  상품 수정
    public void updateFrom(ProductUpdateRequest request, Product product) {
        if (request.getProductName() != null) product.updateProductName(request.getProductName());
        if (request.getProductPrice() != null) product.updateProductPrice(request.getProductPrice());
        if (request.getDescription() != null) product.updateDescription(request.getDescription());
        if (request.getCategory() != null) product.updateCategory(request.getCategory());
        if (request.getStatus() != null) product.updateStatus(request.getStatus());

        if (request.getSaleStartAt() != null || request.getSaleEndAt() != null) {
            SalesPeriod updatedSalesPeriod = SalesPeriod.builder()
                    .startAt(request.getSaleStartAt() != null ? request.getSaleStartAt() : product.getSalesPeriod().getStartAt())
                    .endAt(request.getSaleEndAt() != null ? request.getSaleEndAt() : product.getSalesPeriod().getEndAt())
                    .build();
            product.updatePeriod(updatedSalesPeriod);
        }
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .productPrice(product.getProductPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .status(product.getStatus())
                .stocks(product.getStocks().stream()
                        .map(stock -> ProductStockResponse.builder()
                                .id(stock.getId())
                                .stockQuantity(stock.getStockQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // 내부 서비스 통신용 (e.g. CartService 등에서 사용하는 ProductInternal)
    public ProductInternal toProductInternal(Product product) {
        return ProductInternal.builder()
                .id(product.getId())
                .name(product.getProductName())
                .price(product.getProductPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .status(product.getStatus())
                .build();
    }

    // 내부 재고 조회용 (Feign 응답용 StockInternal)
    public StockInternal toStockInternal(Stock stock) {
        return StockInternal.builder()
                .productId(stock.getProduct().getId())
                .stockQuantity(stock.getStockQuantity())
                .build();
    }
}
