package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.SignupRequest;
import com.app.preorder.memberservice.dto.UpdateMemberRequest;


public interface MemberService {

    Member findByLoginId(String loginId);

    void signUp(SignupRequest signupRequest);

    /* 개인정보 변경 */
    void updateMember(UpdateMemberRequest request, Long memberId);

    /* 비밀번호 변경 */
    void changePassword(Long memberId, String currentPassword, String newPassword);

    /* 아이디 중복 검사 */
    public Long overlapByMemberId(String username);

    /* 이메일 중복 검사 */
    public Long overlapByMemberEmail(String memberEmail);

    /* 휴대폰 중복 검사 */
    public Long overlapByMemberPhone(String memberPhone);

    /* 비밀번호 검증 */
    public MemberInternal verifyPasswordAndGetInfo(String username, String password);

    void confirmEmailVerification(String key);

    void sendSignupVerificationMail(Member member);

}
