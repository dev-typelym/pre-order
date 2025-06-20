package com.app.preorder.productservice.factory;

import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.dto.productDTO.ProductResponse;
import com.app.preorder.productservice.dto.stockDTO.ProductStockResponse;
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
}
