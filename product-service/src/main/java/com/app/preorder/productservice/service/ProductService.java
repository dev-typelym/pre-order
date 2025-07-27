package com.app.preorder.productservice.service;


import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.product.*;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductService {

    //  상품 등록
    Long createProduct(ProductCreateRequest request);

    //  상품 수정
    void updateProduct(Long productId, ProductUpdateRequest request);

    //  상품 삭제
    void deleteProduct(Long productId);

    //  상품 목록
    Page<ProductResponse> getProducts(int page, ProductSearchRequest searchRequest, CategoryType categoryType);

    //  상품 상세 보기
    ProductResponse getProductDetail(Long productId);

    //  상품 단건 조회(feign)
    ProductInternal getProductById(Long productId);

    //  상품 다건 조회(feign)
    List<ProductInternal> getProductsByIds(List<Long> productIds);

    // 상품 ID 목록으로 가용 재고 수량 계산 (재고 - 결제대기수량)
    List<ProductAvailableStockResponse> getAvailableQuantities(List<Long> productIds);


}

