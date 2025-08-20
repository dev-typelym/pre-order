package com.app.preorder.productservice.service;


import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.product.*;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductService {

    // 상품 등록
    Long createProduct(ProductCreateRequest request);

    // 상품 수정
    void updateProduct(Long productId, ProductUpdateRequest request);

    // 상품 삭제
    void deleteProduct(Long productId);

    // 상품 목록 조회(페이지네이션 + 검색/카테고리 필터)
    Page<ProductResponse> getProducts(int page, ProductSearchRequest searchRequest, CategoryType categoryType);

    // 상품 상세 조회
    ProductResponse getProductDetail(Long productId);

    // 상품 단건 내부 조회(Feign용)
    ProductInternal getProductById(Long productId);

    // 상품 다건 내부 조회(Feign용)
    List<ProductInternal> getProductsByIds(List<Long> productIds);

    // 가용재고 단건 조회(숫자만 반환) — 폴링/뱃지용 경량 API
    long getAvailable(Long productId);

    // 가용재고 다건 조회(카트/목록 재동기화용)
    // 가용재고 = stockQuantity - reserved
    List<ProductAvailableStockResponse> getAvailableQuantities(List<Long> productIds);
}
