package com.app.preorder.memberservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMemberInfo {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String addressDetail;
    private String addressSubDetail;
    private String postCode;
}
