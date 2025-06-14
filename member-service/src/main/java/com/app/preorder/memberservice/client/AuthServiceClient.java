package com.app.preorder.memberservice.client;

import com.app.preorder.memberservice.dto.request.LogoutRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", url = "http://auth-service:8080")
public interface AuthServiceClient {

    @PostMapping("/logout")
    void logout(@RequestBody LogoutRequest request);
}
