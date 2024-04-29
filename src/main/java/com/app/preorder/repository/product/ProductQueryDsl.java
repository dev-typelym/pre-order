package com.app.preorder.repository.product;

import com.app.preorder.domain.product.ProductListSearch;
import com.app.preorder.entity.product.Product;
import com.app.preorder.type.CatergoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductQueryDsl {

    //    상품 목록 조회
    public Page<Product> findAllProduct_queryDSL(Pageable pageable, ProductListSearch productListSearch, CatergoryType catergoryType);

    //    상품 상세
    public List<Product> findAllProductDetail_queryDSL();
}
