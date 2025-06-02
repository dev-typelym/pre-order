package com.app.preorder.authservice.service;

public interface AuthService {
    //  로그인
    String login(String username, String password);
    //  로그아웃
    void logout(String refreshToken);
}
