package com.app.preorder.productservice.controller;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.productservice.dto.product.ProductCreateRequest;
import com.app.preorder.productservice.dto.product.ProductUpdateRequest;
import com.app.preorder.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    //  상품 등록
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createProduct(@RequestBody ProductCreateRequest request) {
        Long id = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success(id, "상품이 등록되었습니다."));
    }

    //  상품 수정
    @PatchMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> updateProduct(@PathVariable Long productId,
                                                           @RequestBody ProductUpdateRequest request) {
        productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "상품이 수정되었습니다."));
    }

    //  상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(null, "상품이 삭제되었습니다."));
    }
}
