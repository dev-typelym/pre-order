package com.app.preorder.controller.member;

import com.app.preorder.domain.memberDTO.MemberDTO;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.member.RequestLoginUser;
import com.app.preorder.entity.member.RequestVerifyEmail;
import com.app.preorder.service.member.MemberService;
import com.app.preorder.util.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/member/*")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RedisUtil redisUtil;
    private final EncryptUtil encryptUtil;

    @Autowired
    AuthenticationManager authenticationManager;

    /* 회원 가입 페이지 이동*/
    @GetMapping("signup")
    public String gotoJoinForm(MemberDTO memberDTO) {
        return "/member/signup";
    }

    /* 로그인 페이지 이동*/
    @GetMapping("login")
    public String gotologinForm(MemberDTO memberDTO) {
        return "/member/login";
    }

    /* 회원가입후 login 페이지로 이동*/
    @PostMapping("join")
    public RedirectView join(MemberDTO memberDTO) {
        memberService.signUpUser(memberDTO);
        return new RedirectView("/member/login");
    }

    /* 로그인*/
    @PostMapping("loginProcess")
    public RedirectView login(RequestLoginUser user,
                          HttpServletRequest req,
                          HttpServletResponse res) {
        try {
            final Member member = memberService.loginUser(user.getUsername(), user.getPassword());
            final String token = jwtUtil.generateToken(member);
            final String refreshJwt = jwtUtil.generateRefreshToken(member);
            Cookie accessToken = cookieUtil.createCookie(JwtUtil.ACCESS_TOKEN_NAME, token);
            Cookie refreshToken = cookieUtil.createCookie(JwtUtil.REFRESH_TOKEN_NAME, refreshJwt);
            redisUtil.setDataExpire(refreshJwt, member.getUsername(), JwtUtil.REFRESH_TOKEN_VALIDATION_SECOND);
            res.addCookie(accessToken);
            res.addCookie(refreshToken);
            return new RedirectView("/member/product-list");
        } catch (Exception e) {
            return new RedirectView("/member/login");
        }
    }



    /* 상품 목록으로 이동*/
    @GetMapping("product-list")
    public String gotologinForm() {
        return "/product/product-list";
    }

}
