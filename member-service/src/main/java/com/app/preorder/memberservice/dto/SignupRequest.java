package com.app.preorder.memberservice.dto;

import com.app.preorder.memberservice.domain.vo.Address;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SignupRequest {
    private String loginId;
    private String encodedPassword;
    private String encryptedEmail;
    private String encryptedName;
    private String encryptedPhone;
    private Address address;
}
