package com.app.preorder.productservice.service;


import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.productDTO.ProductListDTO;
import com.app.preorder.productservice.dto.productDTO.ProductListSearch;
import com.app.preorder.productservice.dto.stockDTO.ProductStockDTO;
import com.app.preorder.productservice.domain.entity.Product;
import com.app.preorder.productservice.domain.entity.Stock;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public interface ProductService {



    //  상품 목록
    public Page<ProductListDTO> getProductListWithPaging(int page, ProductListSearch productListSearch, CategoryType categoryType);

    //  상품 상세 보기
    public List<ProductListDTO> getProductDetail();

    //  상품 다건 조회
    public List<ProductInternal> getProductsByIds(List<Long> productIds);

    //  상품 DTO로 바꾸기
    default ProductListDTO toProductListDTO(Product product) {


        return ProductListDTO.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .productPrice(product.getProductPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .productStockDTOS(toProductStockDTO(product.getStocks()))
                .build();
    }

    default List<ProductStockDTO> toProductStockDTO(List<Stock> stocks) {
        return stocks.stream()
                .map(stock -> ProductStockDTO.builder()
                        .id(stock.getId())
                        .stockQuantity(stock.getStockQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    // default 변환 메서드 추가
    default ProductInternal toProductResponse(Product product) {
        return ProductInternal.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productPrice(product.getProductPrice())
                .description(product.getDescription())
                .category(product.getCategory())
                .build();
    }

}

