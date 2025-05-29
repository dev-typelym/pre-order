package com.app.preorder.authservice.client;

@FeignClient(name = "member-service", path = "/internal/members")
public interface MemberServiceClient {

    @GetMapping("/{username}")
    MemberAuthDTO getMemberByUsername(@PathVariable("username") String username);

    @GetMapping("/{username}/salt")
    String getSaltByUsername(@PathVariable("username") String username);
}
