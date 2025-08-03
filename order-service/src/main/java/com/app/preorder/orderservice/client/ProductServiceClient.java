    package com.app.preorder.orderservice.client;

    import com.app.preorder.common.dto.ProductInternal;
    import com.app.preorder.common.dto.StockRequestInternal;
    import com.app.preorder.common.dto.StockInternal;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @FeignClient(name = "product-service", path = "/api/internal/products")
    public interface ProductServiceClient {

        @PostMapping("/list")
        List<ProductInternal> getProductsByIds(@RequestBody List<Long> ids); // ✅ 다건 상품

        @PostMapping("/stocks")
        List<StockInternal> getStocksByIds(@RequestBody List<Long> ids);     // ✅ 다건 재고

        @PatchMapping("/stocks/deduct")
        void deductStocks(@RequestBody List<StockRequestInternal> items);     // ✅ 재고 차감

        @PatchMapping("/stocks/restore")
        void restoreStocks(@RequestBody List<StockRequestInternal> items);
    }

