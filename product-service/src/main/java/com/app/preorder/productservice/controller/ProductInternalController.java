package com.app.preorder.productservice.controller;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.productservice.service.ProductService;
import com.app.preorder.productservice.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/products")
public class ProductInternalController {

    private final ProductService productService;
    private final StockService stockService;

    @PostMapping("/bulk")
    public List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds) {
        return productService.getProductsByIds(productIds);
    }

    @GetMapping("/{productId}")
    public ProductInternal getProductById(@PathVariable Long productId) {
        return productService.getProductById(productId);
    }

    @GetMapping("/{productId}/stock")
    public StockInternal getStockById(@PathVariable Long productId) {
        return stockService.getStockById(productId);
    }
}
