package com.app.preorder.memberservice.controller;


import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.memberservice.client.AuthServiceClient;
import com.app.preorder.memberservice.dto.*;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.repository.MemberRepository;
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
    private final EncryptUtil encryptUtil;


    //  회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signUp(@RequestBody SignupRequest request) {
        memberService.signUp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "회원가입이 완료되었습니다."));
    }

    //  아이디 중복 체크
    @PostMapping("checkId")
    public Long checkId(@RequestParam("username") String username){
        log.info("username: " + username);
        return memberService.overlapByMemberId(username);
    }

    //  이메일 중복 체크
    @PostMapping("checkEmail")
    public Long checkEmail(@RequestParam("memberEmail") String memberEmail){
        log.info("memberEmail: " + memberEmail);
        return memberService.overlapByMemberEmail(memberEmail);
    }

    //  휴대폰 중복 체크
    @PostMapping("checkPhone")
    public Long checkPhone(@RequestParam("memberPhone") String memberPhone){
        log.info("memberPhone: " + memberPhone);
        return memberService.overlapByMemberPhone(memberPhone);
    }


    // 회원 수정
    @PatchMapping("/members/me")
    public ResponseEntity<ApiResponse<Void>> updateMember(@RequestBody UpdateMemberRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenPayload payload = (TokenPayload) authentication.getPrincipal();

        memberService.updateMember(request, payload.getId());

        return ResponseEntity.ok(ApiResponse.success(null, "회원정보가 성공적으로 수정되었습니다."));
    }

    // 비밀번호 변경
    @PatchMapping("/members/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenPayload payload = (TokenPayload) authentication.getPrincipal();
        Long memberId = payload.getId();

        memberService.changePassword(memberId, request.getCurrentPassword(), request.getNewPassword());

        // 로그아웃 요청 (auth-service 호출)
        LogoutRequest logoutRequest = new LogoutRequest(request.getRefreshToken());
        authServiceClient.logout(logoutRequest);

        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호를 변경하였습니다. 다시 로그인 해주세요."));
    }

    // 인증 메일 전송
    @PostMapping("/members/email-verification/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(@RequestBody VerifyEmailRequest request) {
        String encryptedLoginId  = encryptUtil.encrypt(request.getLoginId());
        Member member = memberService.findByLoginId(encryptedLoginId);
        memberService.sendSignupVerificationMail(member);
        return ResponseEntity.ok(ApiResponse.success(null, "성공적으로 인증 메일을 보냈습니다."));
    }

    // 인증 메일 확인
    @GetMapping("/members/verify/{key}")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@PathVariable String key) {
        memberService.confirmEmailVerification(key);
        return ResponseEntity.ok(ApiResponse.success(null, "성공적으로 이메일 인증을 완료했습니다."));
    }

}
