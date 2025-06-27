package com.app.preorder.cartservice.client;

import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.memberservice.dto.response.MemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service")
public interface MemberServiceClient {
    @GetMapping("/api/member/username/{username}")
    MemberInternal getMemberByUsername(@PathVariable String username);
}