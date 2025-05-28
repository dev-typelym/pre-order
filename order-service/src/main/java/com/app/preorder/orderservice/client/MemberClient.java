package com.app.preorder.orderservice.client;

import com.app.preorder.orderservice.domain.member.MemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "member-service", url = "http://member-service:8082")
public interface MemberClient {

    @GetMapping("/api/internal/members/{id}")
    MemberResponse getMemberById(@PathVariable("id") Long id);

    @GetMapping("/api/internal/members/by-username")
    MemberResponse getMemberByUsername(@RequestParam("username") String username);
}
