package com.app.preorder.productservice.service;


import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.product.ProductResponse;
import com.app.preorder.productservice.dto.product.ProductSearchRequest;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductService {

    //  상품 목록
    Page<ProductResponse> getProducts(int page, ProductSearchRequest searchRequest, CategoryType categoryType);

    //  상품 상세 보기
    ProductResponse getProductDetail(Long productId);

    //  상품 단건 조회(feign)
    ProductInternal getProductById(Long productId);

    //  상품 다건 조회(feign)
    List<ProductInternal> getProductsByIds(List<Long> productIds);

}

