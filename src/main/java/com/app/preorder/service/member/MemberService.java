package com.app.preorder.service.member;

import com.app.preorder.domain.memberDTO.MemberDTO;
import com.app.preorder.entity.member.Member;
import com.app.preorder.type.Role;
import javassist.NotFoundException;

public interface MemberService {

    Member findByUsername(String username) throws NotFoundException;

    void signUpUser(MemberDTO memberDTO);

    Member loginUser(String id, String password) throws Exception;

    /* 아이디 중복 검사 */
    public Long overlapByMemberId(String username);

    /* 이메일 중복 검사 */
    public Long overlapByMemberEmail(String memberEmail);

    /* 휴대폰 중복 검사 */
    public Long overlapByMemberPhone(String memberPhone);

    void verifyEmail(String key) throws NotFoundException;

    void sendVerificationMail(Member member) throws NotFoundException;

    void modifyUserRole(Member member, Role userRole);

    default Member toMemberEntity(MemberDTO memberDTO) {
        return Member.builder()
                .username(memberDTO.getUsername())
                .memberPassword(memberDTO.getMemberPassword())
                .memberAddress(memberDTO.getMemberAddress())
                .memberPhone(memberDTO.getMemberPhone())
                .memberEmail(memberDTO.getMemberEmail())
                .memberRole(memberDTO.getMemberRole())
                .memberSleep(memberDTO.getMemberSleep())
                .salt(memberDTO.getSalt())
                .name(memberDTO.getName())
                .memberRegisterDate(memberDTO.getMemberRegisterDate())
                .build();
    }
}