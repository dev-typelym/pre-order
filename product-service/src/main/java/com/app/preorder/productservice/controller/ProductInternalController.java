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

    @PostMapping("/list") // ✅ 상품 다건 조회 (Feign용)
    public List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds) {
        return productService.getProductsByIds(productIds);
    }

    @PostMapping("/stocks") // ✅ 재고 다건 조회 (Feign용)
    public List<StockInternal> getStocksByIds(@RequestBody List<Long> productIds) {
        return stockService.getStocksByIds(productIds);
    }

    @PatchMapping("/stocks/deduct") // ✅ 재고 차감 (Feign용)
    public void deductStocks(@RequestBody List<ProductQuantityDTO> items) {
        stockService.deductStocks(items);
    }
}
