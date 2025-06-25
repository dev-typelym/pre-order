package com.app.preorder.orderservice.client;

import com.app.preorder.common.dto.ProductInternal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/internal/products")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    ProductInternal getProduct(@PathVariable Long productId);

    @GetMapping("/{productId}")
    ProductStockResponse getStock(@PathVariable Long productId);
}
