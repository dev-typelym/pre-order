package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.memberservice.dto.MemberDTO;
import com.app.preorder.common.type.Role;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.UpdateMemberInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.crossstore.ChangeSetPersister;


public interface MemberService {

    Member findByUsername(String username) throws ChangeSetPersister.NotFoundException;

    void signUpUser(MemberDTO memberDTO);

    /* 개인정보 변경 */
    public void updateMember(UpdateMemberInfo updateMemberInfo, Long memberId);

    /* 비밀번호 변경 */
    public void changePassword(String password, Long memberId) throws Exception;

    /* 아이디 중복 검사 */
    public Long overlapByMemberId(String username);

    /* 이메일 중복 검사 */
    public Long overlapByMemberEmail(String memberEmail);

    /* 휴대폰 중복 검사 */
    public Long overlapByMemberPhone(String memberPhone);

    /* 비밀번호 검증 */
    public MemberInternal verifyPasswordAndGetInfo(String username, String password);

    void verifyEmail(String key) throws ChangeSetPersister.NotFoundException;

    void sendVerificationMail(Member member) throws ChangeSetPersister.NotFoundException;

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
