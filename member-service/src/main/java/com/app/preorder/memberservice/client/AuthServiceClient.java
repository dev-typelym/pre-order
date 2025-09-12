package com.app.preorder.memberservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "auth-service", path = "/api/internal/auth")
public interface AuthServiceClient {

    @PostMapping("/logout-all/{memberId}")
    void logoutAll(@PathVariable("memberId") Long memberId);
}
