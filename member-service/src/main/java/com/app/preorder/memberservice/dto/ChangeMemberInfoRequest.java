package com.app.preorder.memberservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeMemberInfoRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String addressDetail;
    private String addressSubDetail;
    private String postCode;
}