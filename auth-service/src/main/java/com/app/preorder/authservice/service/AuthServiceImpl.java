package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.common.dto.VerifyPasswordInternal;
import com.app.preorder.authservice.util.JwtUtil;
import com.app.preorder.authservice.util.RedisUtil;
import com.app.preorder.authservice.exception.custom.InvalidCredentialsException;
import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.authservice.exception.custom.ForbiddenException;
import com.app.preorder.common.type.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberServiceClient memberServiceClient;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    private final long refreshTokenExpireTimeInSeconds = 60 * 60 * 24 * 7;

    public LoginResponse login(LoginRequest loginRequest) {
        MemberInternal member = memberServiceClient.verifyPassword(
                new VerifyPasswordInternal(loginRequest.getUsername(), loginRequest.getPassword())
        );

        if (member == null) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("이메일 인증이 필요합니다.");
        }

        String accessToken = jwtUtil.generateToken(member.getId(), member.getUsername(), member.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(member.getId(), member.getUsername(), member.getRole());

        redisUtil.setDataExpire(refreshToken, member.getUsername(), refreshTokenExpireTimeInSeconds);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        redisUtil.deleteData(logoutRequest.getRefreshToken());
    }
}