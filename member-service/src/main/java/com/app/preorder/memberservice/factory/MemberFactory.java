package com.app.preorder.memberservice.factory;

import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.type.Role;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.infralib.util.HmacHashUtil;
import com.app.preorder.infralib.util.PasswordUtil;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.SignupRequest;
import com.app.preorder.memberservice.dto.UpdateMemberRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MemberFactory {

    private final PasswordUtil passwordUtil;
    private final EncryptUtil encryptUtil;
    private final HmacHashUtil hmacHashUtil;

    public Member createMember(SignupRequest request) {
        return Member.builder()
                .loginId(request.getLoginId())
                .loginIdHash(hmacHashUtil.hmacSha256(request.getLoginId()))
                .password(passwordUtil.encodePassword(request.getPassword()))
                .name(encryptUtil.encrypt(request.getName()))
                .email(encryptUtil.encrypt(request.getEmail()))
                .emailHash(hmacHashUtil.hmacSha256(request.getEmail()))
                .phone(encryptUtil.encrypt(request.getPhone()))
                .phoneHash(hmacHashUtil.hmacSha256(request.getPhone()))
                .address(request.getAddress().encryptWith(encryptUtil))
                .role(Role.ROLE_USER)
                .status(MemberStatus.UNVERIFIED)
                .registeredAt(LocalDateTime.now())
                .build();
    }

    public void updateProfile(Member member, UpdateMemberRequest request) {
        member.updateProfile(
                encryptUtil.encrypt(request.getName()),
                encryptUtil.encrypt(request.getEmail()),
                encryptUtil.encrypt(request.getPhone()),
                request.getAddress().encryptWith(encryptUtil)
        );
    }
}