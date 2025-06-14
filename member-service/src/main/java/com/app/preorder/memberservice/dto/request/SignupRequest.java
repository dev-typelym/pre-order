package com.app.preorder.memberservice.dto.request;

import com.app.preorder.memberservice.domain.vo.Address;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupRequest {
    private String loginId;
    private String password;
    private String email;
    private String name;
    private String phone;
    private Address address;
}
