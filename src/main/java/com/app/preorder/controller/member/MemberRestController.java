package com.app.preorder.controller.member;

import com.app.preorder.domain.memberDTO.MemberDTO;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.member.RequestLoginUser;
import com.app.preorder.entity.member.RequestVerifyEmail;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.service.member.MemberService;
import com.app.preorder.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;



import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/members/*")
@Slf4j
@RequiredArgsConstructor
public class MemberRestController {
    @Autowired
    private MemberService memberService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CookieUtil cookieUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private EncryptUtil encryptUtil;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    private MemberRepository memberRepository;


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


//    /* 로그인*/
//    @PostMapping("loginProcess")
//    public Response login(@RequestBody RequestLoginUser user,
//                          HttpServletRequest req,
//                          HttpServletResponse res) {
//        try {
//            final Member member = memberService.loginUser(user.getUsername(), user.getPassword());
//            final String token = jwtUtil.generateToken(member);
//            final String refreshJwt = jwtUtil.generateRefreshToken(member);
//            Cookie accessToken = cookieUtil.createCookie(JwtUtil.ACCESS_TOKEN_NAME, token);
//            Cookie refreshToken = cookieUtil.createCookie(JwtUtil.REFRESH_TOKEN_NAME, refreshJwt);
//            redisUtil.setDataExpire(refreshJwt, member.getUsername(), JwtUtil.REFRESH_TOKEN_VALIDATION_SECOND);
//            res.addCookie(accessToken);
//            res.addCookie(refreshToken);
//            return new Response("success", "로그인에 성공했습니다.", token);
//        } catch (Exception e) {
//            return new Response("error", "로그인에 실패했습니다.", e.getMessage());
//        }
//    }

    // 로그아웃
    @GetMapping
    public Response logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            memberService.logoutUser(request, response);
            return new Response("success", "로그아웃 성공.", "로그아웃 성공");
        } catch (Exception e) {
            return new Response("error", "로그아웃 실패.", e.getMessage());
        }
    }

    // 개인정보 변경
    @PostMapping("changMemberInfo")
    public Response changPassword(String name, String email, String phone, String address, String addressDetail, String addressSubDetail, String postCode) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);

        try {
            memberService.changeMemberInfo(name, email, phone, address, addressDetail, addressSubDetail, postCode, member.getId());
            return new Response("success", "개인정보를 변경하였습니다.", "개인정보 변경 성공");
        } catch (Exception e) {
            return new Response("error", "개인정보 변경에 실패했습니다.", e.getMessage());
        }
    }

    // 비밀번호 변경
    @PostMapping("changPassword")
    public Response changPassword(String password) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);

        try {
            memberService.changePassword(password, member.getId());
            return new Response("success", "비밀번호 변경하였습니다.", "비밀번호 성공");
        } catch (Exception e) {
            return new Response("error", "비밀번호 변경에 실패했습니다.", e.getMessage());
        }
    }

    /* 인증 이메일 보내기*/
    @PostMapping("verify")
    public Response verify(RequestVerifyEmail verifyEmail, HttpServletRequest req, HttpServletResponse res) {
        Response response;
        try {
            Member member = memberService.findByUsername(encryptUtil.encrypt(verifyEmail.getUsername()));
            memberService.sendVerificationMail(member);
            response = new Response("success", "성공적으로 인증메일을 보냈습니다.", null);
        } catch (Exception exception) {
            response = new Response("error", "인증메일을 보내는데 문제가 발생했습니다.", exception);
        }
        return response;
    }

    /* 이메일 인증 확인*/
    @GetMapping("/verify/{key}")
    public Response getVerify(@PathVariable String key) {
        Response response;
        try {
            memberService.verifyEmail(key);
            response = new Response("success", "성공적으로 인증메일을 확인했습니다.", null);

        } catch (Exception e) {
            response = new Response("error", "인증메일을 확인하는데 실패했습니다.", null);
        }
        return response;
    }

}
