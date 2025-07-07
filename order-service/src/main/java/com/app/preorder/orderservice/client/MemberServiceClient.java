package com.app.preorder.orderservice.client;

import com.app.preorder.orderservice.domain.member.MemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "member-service", path = "/api/internal/members")
public interface MemberServiceClient {

    @GetMapping("/{id}")
    MemberResponse getMemberById(@PathVariable("id") Long id);

    @GetMapping("/by-username")
    MemberResponse getMemberByUsername(@RequestParam("username") String username);
}
