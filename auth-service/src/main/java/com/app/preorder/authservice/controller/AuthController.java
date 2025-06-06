package com.app.preorder.authservice.controller;

import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.authservice.service.AuthService;
import com.app.preorder.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 사용자 로그인 및 토큰 발급
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(
                ApiResponse.<LoginResponse>builder()
                        .success(true)
                        .message("로그인 성공")
                        .data(response)
                        .build()
        );
    }

    // 사용자 로그아웃 (RefreshToken 폐기)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("로그아웃 성공")
                        .data(null)
                        .build()
        );
    }
}