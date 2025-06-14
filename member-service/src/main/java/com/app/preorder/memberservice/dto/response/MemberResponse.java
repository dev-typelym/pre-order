package com.app.preorder.memberservice.dto.response;

import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.type.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {

    private Long id;
    private String loginId;
    private MemberStatus status;
    private Role role;

}
