package com.app.preorder.memberservice.factory;

import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.common.type.Role;
import com.app.preorder.infralib.util.EncryptUtil;
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

    public Member createMember(SignupRequest request) {
        return Member.builder()
                .loginId(encryptUtil.encrypt(request.getLoginId()))
                .password(passwordUtil.encodePassword(request.getEncodedPassword()))
                .name(encryptUtil.encrypt(request.getEncryptedName()))
                .email(encryptUtil.encrypt(request.getEncryptedEmail()))
                .phone(encryptUtil.encrypt(request.getEncryptedPhone()))
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