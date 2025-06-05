package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.LoginResponse;
import com.app.preorder.authservice.dto.VerifyPasswordRequest;
import com.app.preorder.authservice.util.JwtUtil;
import com.app.preorder.authservice.util.RedisUtil;
import com.app.preorder.common.exception.InvalidPasswordException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberServiceClient memberServiceClient;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    private final long refreshTokenExpireTimeInSeconds = 60 * 60 * 24 * 7;

    @Override
    public LoginResponse login(String username, String password) {
        boolean isValid = memberServiceClient.verifyPassword(new VerifyPasswordRequest(username, password));

        if (!isValid) {
            throw new InvalidPasswordException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.generateToken(username);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        redisUtil.setDataExpire(refreshToken, username, refreshTokenExpireTimeInSeconds);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        redisUtil.deleteData(refreshToken);
    }
}