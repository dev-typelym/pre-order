    package com.app.preorder.orderservice.client;

    import com.app.preorder.common.dto.ProductInternal;
    import com.app.preorder.common.dto.StockRequestInternal;
    import com.app.preorder.common.dto.StockInternal;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @FeignClient(name = "product-service", path = "/api/internal/products")
    public interface ProductServiceClient {

        // 조회
        @PostMapping("/list")
        List<ProductInternal> getProductsByIds(@RequestBody List<Long> ids);

        @PostMapping("/stocks")
        List<StockInternal> getStocksByIds(@RequestBody List<Long> ids);

        // 🔁 기존 deduct/restore → ❌ deduct 삭제 / ✅ reserve·unreserve·commit 사용
        @PostMapping("/stocks/reserve")
        void reserveStocks(@RequestBody List<StockRequestInternal> items);

        @PostMapping("/stocks/unreserve")
        void unreserveStocks(@RequestBody List<StockRequestInternal> items);

        @PatchMapping("/stocks/commit")
        void commitStocks(@RequestBody List<StockRequestInternal> items);

        // 결제 후 환불/반품(재입고)
        @PatchMapping("/stocks/restore")
        void restoreStocks(@RequestBody List<StockRequestInternal> items);
    }

