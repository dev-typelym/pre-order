package com.app.preorder.authservice.client;

import com.app.preorder.authservice.dto.request.VerifyPasswordRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "member-service", path = "/internal/members")
public interface MemberServiceClient {

    @PostMapping("/verify-password")
    boolean verifyPassword(@RequestBody VerifyPasswordRequest request);
}
