package com.app.preorder.authservice.controller;

import com.app.preorder.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/internal/auth")
@RequiredArgsConstructor
public class AuthInternalController {

    private final AuthService authService;

    /** 비밀번호 변경 직후: 회원의 모든 기기 로그아웃 */
    @PostMapping("/logout-all/{memberId}")
    public void logoutAll(@PathVariable Long memberId) {
        authService.logoutAllDevices(memberId);
    }
}
