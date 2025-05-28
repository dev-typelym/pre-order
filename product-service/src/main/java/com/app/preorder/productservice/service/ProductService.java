package com.app.preorder.productservice.service;

import com.app.preorder.domain.productDTO.ProductListDTO;
import com.app.preorder.domain.productDTO.ProductListSearch;
import com.app.preorder.domain.stockDTO.ProductStockDTO;
import com.app.preorder.entity.product.Product;
import com.app.preorder.entity.product.Stock;
import com.app.preorder.type.CatergoryType;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public interface ProductService {

    //   상품 목록
    public Page<ProductListDTO> getProductListWithPaging(int page, ProductListSearch productListSearch, CatergoryType catergoryType);

    //    상품 상세 보기
    public List<ProductListDTO> getProductDetail();

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
}

