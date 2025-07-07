package com.app.preorder.cartservice.client;

import com.app.preorder.common.dto.ProductInternal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service", path = "/api/internal/products")
public interface ProductServiceClient {

    @PostMapping("/list")
    List<ProductInternal> getProductsByIds(@RequestBody List<Long> productIds);
}

