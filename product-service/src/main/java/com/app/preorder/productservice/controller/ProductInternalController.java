package com.app.preorder.productservice.controller;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockRequestInternal;
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

    // ✅ 상품 다건 조회 (Feign용)
    @PostMapping("/list")
    public List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds) {
        return productService.getProductsByIds(productIds);
    }

    // ✅ 재고 다건 조회 (Feign용)
    @PostMapping("/stocks")
    public List<StockInternal> getStocksByIds(@RequestBody List<Long> productIds) {
        return stockService.getStocksByIds(productIds);
    }

    // ✅ 예약 잡기 (prepare 단계)
    @PostMapping("/stocks/reserve")
    public void reserveStocks(@RequestBody List<StockRequestInternal> items) {
        stockService.reserveStocks(items);
    }

    // ✅ 예약 해제 (결제 전 취소/이탈 정리)
    @PostMapping("/stocks/unreserve")
    public void unreserveStocks(@RequestBody List<StockRequestInternal> items) {
        stockService.unreserveStocks(items);
    }

    // ✅ 커밋(= consumeReserved, complete 단계 한 방 처리: Q-=, R-=)
    @PatchMapping("/stocks/commit")
    public void commitStocks(@RequestBody List<StockRequestInternal> items) {
        stockService.commitStocks(items);
    }

    // ✅ 재고 복원(결제 후 보상: 환불/반품)
    @PatchMapping("/stocks/restore")
    public void restoreStocks(@RequestBody List<StockRequestInternal> items) {
        stockService.restoreStocks(items);
    }
}
