package com.app.preorder.productservice.factory;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.dto.product.ProductResponse;
import com.app.preorder.productservice.dto.stock.ProductStockResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductFactory {

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .productPrice(product.getProductPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .stocks(product.getStocks().stream()
                        .map(stock -> ProductStockResponse.builder()
                                .id(stock.getId())
                                .stockQuantity(stock.getStockQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // 내부 서비스 통신용 (e.g. CartService 등에서 사용하는 ProductInternal)
    public ProductInternal toInternal(Product product) {
        return ProductInternal.builder()
                .id(product.getId())
                .name(product.getProductName())
                .price(product.getProductPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .build();
    }
}
