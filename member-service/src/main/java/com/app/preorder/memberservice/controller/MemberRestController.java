package com.app.preorder.memberservice.controller;


import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.memberservice.dto.UpdateMemberInfo;
import com.app.preorder.memberservice.dto.UpdateMemberRequest;
import com.app.preorder.memberservice.dto.RequestVerifyEmailDTO;
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
import org.springframework.http.HttpStatus;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/members")
@Slf4j
@RequiredArgsConstructor
public class MemberRestController {

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
    @PostMapping("changePassword")
    public ResponseEntity<Response> changePassword(@RequestParam String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        Member member = memberRepository.findByUsername(currentUsername);

        try {
            memberService.changePassword(password, member.getId());
            memberService.logoutUser(request, response);
            return ResponseEntity.ok(new Response("success", "비밀번호 변경하였습니다.", "비밀번호 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response("error", "비밀번호 변경에 실패했습니다.", e.getMessage()));
        }
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
