package com.app.preorder.orderservice.client;

import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.orderservice.client.config.MemberFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "member-service", path = "/api/internal/members",  configuration = MemberFeignConfig.class)
public interface MemberServiceClient {

    @GetMapping("/{memberId}")
    MemberInternal getMemberById(@PathVariable("memberId") Long id);

    @GetMapping("/by-username")
    MemberInternal getMemberByUsername(@RequestParam("username") String username);
}
