package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.common.dto.VerifyPasswordInternal;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.exception.custom.ForbiddenException;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.infralib.util.JwtUtil;
import com.app.preorder.infralib.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final MemberServiceClient memberServiceClient;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    private final long refreshTokenExpireTimeInSeconds = 60 * 60 * 24 * 7;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        MemberInternal member;

        try {
            member = memberServiceClient.verifyPassword(
                    new VerifyPasswordInternal(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (feign.FeignException e) {
            log.error("[AuthService] 로그인 중 회원 서비스 통신 실패 - loginId: {}, 사유: {}", loginRequest.getUsername(), e.getMessage());
            throw new FeignException("회원 서비스 통신 실패", e);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("이메일 인증이 필요합니다.");
        }

        String accessToken = jwtUtil.generateToken(member.getId(), member.getLoginId(), member.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(member.getId(), member.getLoginId(), member.getRole().name());

        redisUtil.setDataExpire(refreshToken, member.getLoginId(), refreshTokenExpireTimeInSeconds);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        redisUtil.deleteData(logoutRequest.getRefreshToken());
    }
}