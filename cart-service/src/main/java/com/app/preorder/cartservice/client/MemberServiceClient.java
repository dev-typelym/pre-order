package com.app.preorder.cartservice.client;

import com.app.preorder.cartservice.dto.member.MemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service")
public interface MemberServiceClient {
    @GetMapping("/api/member/username/{username}")
    MemberResponse getMemberByUsername(@PathVariable String username);
}