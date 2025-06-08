package com.app.preorder.memberservice.controller;


import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.memberservice.client.AuthServiceClient;
import com.app.preorder.memberservice.dto.*;
import com.app.preorder.memberservice.domain.entity.Member;
import com.app.preorder.memberservice.repository.MemberRepository;
import com.app.preorder.memberservice.service.member.MemberService;
import com.app.preorder.memberservice.util.EncryptUtil;
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
    private final MemberRepository memberRepository;


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
        Long memberId = payload.getId();
        
        UpdateMemberInfo updateMemberInfo = UpdateMemberInfo.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .addressSubDetail(request.getAddressSubDetail())
                .postCode(request.getPostCode())
                .build();

        memberService.updateMember(updateMemberInfo, memberId);

        return ResponseEntity.ok(ApiResponse.success(null, "개인정보를 변경하였습니다."));
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

    /* 인증 이메일 보내기*/
    @PostMapping("/verify")
    public ApiResponse<Void> verify(@RequestBody RequestVerifyEmailDTO verifyEmail) {
        Member member = memberService.findByUsername(encryptUtil.encrypt(verifyEmail.getUsername()));
        memberService.sendVerificationMail(member);
        return ApiResponse.success(null, "성공적으로 인증메일을 보냈습니다.");
    }

    /* 이메일 인증 확인*/
    @GetMapping("/verify/{key}")
    public ApiResponse<Void> getVerify(@PathVariable String key) {
        memberService.verifyEmail(key);
        return ApiResponse.success(null, "성공적으로 인증메일을 확인했습니다.");
    }

}
