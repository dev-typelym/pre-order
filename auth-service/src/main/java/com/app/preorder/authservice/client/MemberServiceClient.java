package com.app.preorder.authservice.client;

import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.dto.VerifyPasswordInternal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "member-service", path = "/api/internal/members")
public interface MemberServiceClient {

    @PostMapping("/verify-password")
    MemberInternal verifyPassword(@RequestBody VerifyPasswordInternal request);
}
