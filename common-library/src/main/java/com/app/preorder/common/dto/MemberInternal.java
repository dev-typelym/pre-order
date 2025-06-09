package com.app.preorder.common.dto;

import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.type.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberInternal {
    private Long id;
    private String loginId;
    private MemberStatus status;
    private Role role;

    public static MemberInternal of(Long id, String loginId, MemberStatus status, Role role) {
        return new MemberInternal(id, loginId, status, role);
    }
}
