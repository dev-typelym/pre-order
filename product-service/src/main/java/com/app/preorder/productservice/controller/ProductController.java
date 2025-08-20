package com.app.preorder.productservice.controller;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.product.ProductAvailableStockResponse;
import com.app.preorder.productservice.dto.product.ProductResponse;
import com.app.preorder.productservice.dto.product.ProductSearchRequest;
import com.app.preorder.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    // 상품 목록 조회(페이지네이션 + 검색/카테고리 필터)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @ModelAttribute ProductSearchRequest searchRequest,
            @RequestParam(required = false) CategoryType categoryType
    ) {
        Page<ProductResponse> result = productService.getProducts(page - 1, searchRequest, categoryType);
        return ResponseEntity.ok(ApiResponse.success(result, "상품 목록 조회 성공"));
    }

    // 상품 상세 조회(현재 시점 가용재고 포함)
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(@PathVariable Long productId) {
        ProductResponse result = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(result, "상품 상세 조회 성공"));
    }

    // 가용재고 단건 조회(숫자만 반환, 폴링/뱃지용 경량 API)
    @GetMapping("/{productId}/available")
    public ResponseEntity<ApiResponse<Long>> getAvailable(@PathVariable Long productId) {
        long result = productService.getAvailable(productId);
        return ResponseEntity.ok(ApiResponse.success(result, "가용 재고 수량 조회 성공"));
    }

    // 가용재고 다건 조회(카트/목록 재동기화용)
    @PostMapping("/available-quantities")
    public ResponseEntity<ApiResponse<List<ProductAvailableStockResponse>>> getAvailableQuantities(@RequestBody List<Long> productIds) {
        List<ProductAvailableStockResponse> result = productService.getAvailableQuantities(productIds);
        return ResponseEntity.ok(ApiResponse.success(result, "가용 재고 수량 조회 성공"));
    }

}
