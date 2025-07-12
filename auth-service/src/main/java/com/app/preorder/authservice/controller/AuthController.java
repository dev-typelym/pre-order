package com.app.preorder.authservice.controller;

import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.request.TokenRequest;
import com.app.preorder.authservice.dto.response.AuthUserResponse;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.authservice.dto.response.TokenResponse;
import com.app.preorder.authservice.service.AuthService;
import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //  사용자 로그인 및 토큰 발급
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"));
    }

    //  사용자 로그아웃 (RefreshToken 폐기)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody LogoutRequest logoutRequest) {
        authService.logout(logoutRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 성공"));
    }

    //  인증된 사용자 기본 정보 조회 (프론트 로그인 상태 확인용)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> getAuthUserInfo() {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AuthUserResponse response = AuthUserResponse.of(payload);
        return ResponseEntity.ok(ApiResponse.success(response, "인증된 사용자 정보 조회 성공"));
    }

    //  Refresh Token을 이용한 Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@RequestBody TokenRequest request) {
        TokenResponse response = authService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "토큰 재발급 성공"));
    }

    //  Refresh Token 상태 확인 (서버에 저장된 Refresh Token 존재 여부 확인)
    @GetMapping("/refresh-token/status")
    public ResponseEntity<ApiResponse<Boolean>> checkRefreshTokenStatus() {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean status = authService.checkRefreshTokenStatus(payload.getId());
        return ResponseEntity.ok(ApiResponse.success(status, "Refresh Token 상태 확인 성공"));
    }
}