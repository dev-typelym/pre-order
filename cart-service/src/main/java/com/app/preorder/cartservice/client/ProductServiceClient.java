package com.app.preorder.cartservice.client;

import com.app.preorder.common.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "product-service", url = "${msa.product-service.url}")
public interface ProductServiceClient {

    @GetMapping("/products/{productId}")
    ProductResponse getProductById(@PathVariable("productId") Long productId);

    @PostMapping("/products/bulk")
    List<ProductResponse> getProductsByIds(@RequestBody List<Long> productIds);
}

