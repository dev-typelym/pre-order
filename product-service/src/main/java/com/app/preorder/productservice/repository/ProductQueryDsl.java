package com.app.preorder.productservice.repository;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.productDTO.ProductListSearch;
import com.app.preorder.productservice.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductQueryDsl {

    //    상품id로 상품 조회
    public Product findProductByProductId_queryDSL(Long productId);

    //    상품 목록 조회
    public Page<Product> findAllProduct_queryDSL(Pageable pageable, ProductListSearch productListSearch, CategoryType categoryType);

    //    상품 상세
    public List<Product> findAllProductDetail_queryDSL();
}
