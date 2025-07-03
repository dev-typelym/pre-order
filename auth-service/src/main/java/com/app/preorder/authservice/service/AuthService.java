package com.app.preorder.authservice.service;

import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.authservice.dto.response.TokenResponse;

public interface AuthService {
    //  로그인
    LoginResponse login(LoginRequest loginRequest);
    //  로그아웃
    void logout(LogoutRequest logoutRequest);
    TokenResponse reissue(String refreshToken);
    boolean checkRefreshTokenStatus(Long memberId);
}
