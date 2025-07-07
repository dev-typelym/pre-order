package com.app.preorder.memberservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cart-service", path = "/api/internal/carts")
public interface CartServiceClient {

    @PostMapping("")
    void createCart(@RequestBody Long memberId);
}
