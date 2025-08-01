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

    // 상품 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @ModelAttribute ProductSearchRequest searchRequest,
            @RequestParam(required = false) CategoryType categoryType
    ) {
        Page<ProductResponse> result = productService.getProducts(page - 1, searchRequest, categoryType);
        return ResponseEntity.ok(ApiResponse.success(result, "상품 목록 조회 성공"));
    }

    // 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductDetail(@PathVariable Long productId) {
        ProductResponse result = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(result, "상품 상세 조회 성공"));
    }

    // 상품 가용 재고 수량 조회 (다건)
    @PostMapping("/available-quantities")
    public ResponseEntity<ApiResponse<List<ProductAvailableStockResponse>>> getAvailableQuantities(@RequestBody List<Long> productIds) {
        List<ProductAvailableStockResponse> response = productService.getAvailableQuantities(productIds);
        return ResponseEntity.ok(ApiResponse.success(response, "가용 재고 수량 조회 성공"));
    }

}
