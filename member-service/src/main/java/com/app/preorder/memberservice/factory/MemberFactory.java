package com.app.preorder.memberservice.factory;

import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.type.Role;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.infralib.util.HmacHashUtil;
import com.app.preorder.infralib.util.PasswordUtil;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.domain.vo.Address;
import com.app.preorder.memberservice.dto.request.SignupRequest;
import com.app.preorder.memberservice.dto.request.UpdateMemberRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.trim;

@Component
@RequiredArgsConstructor
public class MemberFactory {

    private final PasswordUtil passwordUtil;
    private final EncryptUtil encryptUtil;
    private final HmacHashUtil hmacHashUtil;

    public Member createMember(SignupRequest request) {
        return Member.builder()
                .loginId(trim(request.getLoginId()))
                .loginIdHash(hmacHashUtil.hmacSha256(trim(request.getLoginId())))
                .password(passwordUtil.encodePassword(trim(request.getPassword())))
                .name(encryptUtil.encrypt(trim(request.getName())))
                .email(encryptUtil.encrypt(trim(request.getEmail())))
                .emailHash(hmacHashUtil.hmacSha256(trim(request.getEmail())))
                .phone(encryptUtil.encrypt(trim(request.getPhone())))
                .phoneHash(hmacHashUtil.hmacSha256(trim(request.getPhone())))
                .address(new Address(
                        encryptUtil.encrypt(trim(request.getAddress().getRoadAddress())),
                        encryptUtil.encrypt(trim(request.getAddress().getDetailAddress())),
                        encryptUtil.encrypt(trim(request.getAddress().getPostalCode()))
                ))
                .role(Role.ROLE_USER)
                .status(MemberStatus.UNVERIFIED)
                .build();
    }

    public void updateProfile(Member member, UpdateMemberRequest request) {
        member.updateProfile(
                encryptUtil.encrypt(trim(request.getName())),
                encryptUtil.encrypt(trim(request.getEmail())),
                encryptUtil.encrypt(trim(request.getPhone())),
                new Address(
                        encryptUtil.encrypt(trim(request.getAddress().getRoadAddress())),
                        encryptUtil.encrypt(trim(request.getAddress().getDetailAddress())),
                        encryptUtil.encrypt(trim(request.getAddress().getPostalCode()))
                )
        );
    }

}