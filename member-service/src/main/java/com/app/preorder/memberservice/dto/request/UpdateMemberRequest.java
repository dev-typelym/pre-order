package com.app.preorder.memberservice.dto.request;

import com.app.preorder.memberservice.domain.vo.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {
    private String name;
    private String email;
    private String phone;
    private Address address;
}