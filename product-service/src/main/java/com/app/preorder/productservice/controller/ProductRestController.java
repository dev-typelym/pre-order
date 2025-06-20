package com.app.preorder.productservice.controller;

import com.app.preorder.common.type.CategoryType;
import com.app.preorder.productservice.dto.productDTO.ProductResponse;
import com.app.preorder.productservice.dto.productDTO.ProductSearchRequest;
import com.app.preorder.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductRestController {

    private final ProductService productService;


    //  상품 목록
    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @ModelAttribute ProductSearchRequest searchRequest,
            @RequestParam(required = false) CategoryType categoryType
    ) {
        return productService.getProducts(page - 1, searchRequest, categoryType);
    }

    //  상품 상세
    @GetMapping("/{productId}")
    public ProductResponse getProductDetail(@PathVariable Long productId) {
        return productService.getProductDetail(productId);
    }

}
