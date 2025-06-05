package com.app.preorder.authservice.service;

import com.app.preorder.authservice.dto.LoginResponse;

public interface AuthService {
    //  로그인
    LoginResponse login(String username, String password);
    //  로그아웃
    void logout(String refreshToken);
}
