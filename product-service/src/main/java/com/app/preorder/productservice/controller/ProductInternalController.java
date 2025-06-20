package com.app.preorder.productservice.controller;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/products")
public class ProductInternalController {

    private final ProductService productService;

    @PostMapping("/bulk")
    public List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds) {
        return productService.getProductsByIds(productIds);
    }
}
