    package com.app.preorder.orderservice.client;

    import com.app.preorder.common.dto.ProductInternal;
    import com.app.preorder.common.dto.StockRequestInternal;
    import com.app.preorder.orderservice.client.config.ProductFeignConfig;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @FeignClient(name = "product-service", path = "/api/internal/products", configuration = ProductFeignConfig.class)
    public interface ProductServiceClient {

        // 조회성
        @PostMapping("/list")
        List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds);

        @PostMapping("/stocks")
        List<ProductInternal> getStocks(@RequestBody List<Long> productIds);

        // 상태 변경(재고)
        @PostMapping("/stocks/reserve")
        void reserveStocks(@RequestBody List<StockRequestInternal> items);

        @PostMapping("/stocks/unreserve")
        void unreserveStocks(@RequestBody List<StockRequestInternal> items);

        @PatchMapping("/stocks/commit")
        void commitStocks(@RequestBody List<StockRequestInternal> items);

        @PatchMapping("/stocks/restore")
        void restoreStocks(@RequestBody List<StockRequestInternal> items);
    }

