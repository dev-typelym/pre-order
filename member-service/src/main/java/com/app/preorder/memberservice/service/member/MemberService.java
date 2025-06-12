package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.SignupRequest;
import com.app.preorder.memberservice.dto.UpdateMemberRequest;
import com.app.preorder.common.type.DuplicateCheckType;


public interface MemberService {

    Member findByLoginId(String loginId);

    void signUp(SignupRequest signupRequest);

    /* 개인정보 변경 */
    void updateMember(UpdateMemberRequest request, Long memberId);

    /* 비밀번호 변경 */
    void changePassword(Long memberId, String currentPassword, String newPassword);

    /* 중복 체크 */
    boolean isDuplicate(DuplicateCheckType type, String value);

    /* 비밀번호 검증 */
    public MemberInternal verifyPasswordAndGetInfo(String username, String password);

    void confirmEmailVerification(String key);

    void sendSignupVerificationMail(Member member);

}
