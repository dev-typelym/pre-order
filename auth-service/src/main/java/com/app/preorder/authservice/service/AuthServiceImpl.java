package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.authservice.dto.request.VerifyPasswordRequest;
import com.app.preorder.authservice.util.JwtUtil;
import com.app.preorder.authservice.util.RedisUtil;
import com.app.preorder.authservice.exception.custom.InvalidPasswordException;
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
    public LoginResponse login(LoginRequest loginRequest) {
        boolean isValid = memberServiceClient.verifyPassword(new VerifyPasswordRequest(loginRequest.getUsername(), loginRequest.getPassword()));

        if (!isValid) {
            throw new InvalidPasswordException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.generateToken(loginRequest.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(loginRequest.getUsername());
        redisUtil.setDataExpire(refreshToken, loginRequest.getUsername(), refreshTokenExpireTimeInSeconds);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        redisUtil.deleteData(logoutRequest.getRefreshToken());
    }
}