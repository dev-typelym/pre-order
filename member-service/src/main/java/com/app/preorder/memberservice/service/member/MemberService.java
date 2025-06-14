package com.app.preorder.memberservice.service.member;


import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.dto.request.DuplicateCheckRequest;
import com.app.preorder.memberservice.dto.request.SignupRequest;
import com.app.preorder.memberservice.dto.request.UpdateMemberRequest;


public interface MemberService {

    /** 회원 단건 조회 */
    Member findByLoginId(String loginId);

    /** 회원가입 */
    void signUp(SignupRequest signupRequest);

    /** 회원 정보 수정 */
    void updateMember(UpdateMemberRequest request, Long memberId);

    /** 비밀번호 변경 */
    void changePassword(Long memberId, String currentPassword, String newPassword);

    /** 중복 여부 확인 */
    String checkDuplicate(DuplicateCheckRequest request);

    /** 비밀번호 검증 및 내부 정보 반환 */
    MemberInternal verifyPasswordAndGetInfo(String username, String password);

    /** 이메일 인증 확인 */
    void confirmEmailVerification(String key);

    /** 이메일 인증 메일 발송 */
    void sendSignupVerificationMail(String loginId);
}
