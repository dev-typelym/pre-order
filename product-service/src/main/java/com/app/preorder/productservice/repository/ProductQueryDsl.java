package com.app.preorder.productservice.repository;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.product.ProductSearchRequest;
import com.app.preorder.productservice.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductQueryDsl {

    //    상품 목록 조회
    Page<Product> findAllBySearchConditions(Pageable pageable, ProductSearchRequest searchRequest, CategoryType categoryType);

    //    상품 상세
    Optional<Product> findByIdWithStocks(Long productId);
}
