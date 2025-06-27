package com.app.preorder.orderservice.client;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockInternal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", path = "/api/internal/products")
public interface ProductServiceClient {

    @GetMapping("/{productId}")
    ProductInternal getProductById(@PathVariable Long productId);   // ✅ 단건 상품 조회

    @GetMapping("/{productId}/stock")
    StockInternal getStockById(@PathVariable Long productId);
}
