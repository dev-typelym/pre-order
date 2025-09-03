    package com.app.preorder.orderservice.client;

    import com.app.preorder.common.dto.ProductInternal;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @FeignClient(name = "product-service", path = "/api/internal/products")
    public interface ProductServiceClient {

        // 조회성
        @PostMapping("/list")
        List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds);

    }

