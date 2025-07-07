package com.app.preorder.memberservice.controller;


import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.memberservice.client.AuthServiceClient;
import com.app.preorder.memberservice.dto.request.*;
import com.app.preorder.memberservice.dto.response.MemberDetailResponse;
import com.app.preorder.memberservice.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/members")
@Slf4j
@RequiredArgsConstructor
public class MemberRestController {

    private final AuthServiceClient authServiceClient;
    private final MemberService memberService;

    /** 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody SignupRequest request) {
        memberService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(null, "회원가입이 완료되었습니다."));
    }

    /** 중복 체크 */
    @PostMapping("/check-duplicate")
    public ResponseEntity<ApiResponse<Void>> checkDuplicate(@RequestBody DuplicateCheckRequest request) {
        String message = memberService.checkDuplicate(request);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    /** 내 정보 조회 */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberDetailResponse>> getMyInfo() {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MemberDetailResponse response = memberService.getMyInfo(payload.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "회원 정보 조회 성공"));
    }

    /** 회원 정보 수정 */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMember(@RequestBody UpdateMemberRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenPayload payload = (TokenPayload) authentication.getPrincipal();
        memberService.updateMember(request, payload.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "회원정보가 성공적으로 수정되었습니다."));
    }

    /** 비밀번호 변경 */
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenPayload payload = (TokenPayload) authentication.getPrincipal();
        memberService.changePassword(payload.getId(), request.getCurrentPassword(), request.getNewPassword());
        authServiceClient.logout(new LogoutRequest(request.getRefreshToken()));
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호를 변경하였습니다. 다시 로그인 해주세요."));
    }

    /** 인증 메일 전송 */
    @PostMapping("/email/verification/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(@RequestBody VerifyEmailRequest request) {
        memberService.sendSignupVerificationMail(request.getLoginId());
        return ResponseEntity.ok(ApiResponse.success(null, "성공적으로 인증 메일을 보냈습니다."));
    }

    /** 인증 메일 확인 */
    @GetMapping("/email/verification/{key}")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@PathVariable String key) {
        memberService.confirmEmailVerification(key);
        return ResponseEntity.ok(ApiResponse.success(null, "성공적으로 이메일 인증을 완료했습니다."));
    }

    /** 회원 탈퇴 */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@RequestBody DeleteMemberRequest request) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        memberService.deleteMember(payload.getId(), request.getCurrentPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "회원 탈퇴가 완료되었습니다."));
    }
}
