package com.app.preorder.memberservice.dto.response;

import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.memberservice.domain.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberDetailResponse {
    private Long id;
    private String loginId;
    private String name;
    private String email;
    private String phone;
    private MemberStatus status;
    private AddressResponse address;

    public static MemberDetailResponse of(Member member, EncryptUtil encryptUtil) {
        return new MemberDetailResponse(
                member.getId(),
                member.getLoginId(),
                encryptUtil.decrypt(member.getName()),
                encryptUtil.decrypt(member.getEmail()),
                encryptUtil.decrypt(member.getPhone()),
                member.getStatus(),
                AddressResponse.of(member.getAddress(), encryptUtil)
        );
    }
}
