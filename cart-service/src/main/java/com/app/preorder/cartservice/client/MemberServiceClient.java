package com.app.preorder.cartservice.client;

import com.app.preorder.common.dto.MemberInternal;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", path = "/api/internal/members")
public interface MemberServiceClient {

    @GetMapping("/username/{username}")
    MemberInternal getMemberByUsername(@PathVariable String username);
}