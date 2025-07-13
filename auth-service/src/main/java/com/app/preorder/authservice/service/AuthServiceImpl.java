package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.authservice.dto.response.TokenResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.common.dto.VerifyPasswordInternal;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.exception.custom.ForbiddenException;
import com.app.preorder.common.exception.custom.RefreshTokenException;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.infralib.util.JwtUtil;
import com.app.preorder.infralib.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.trim;


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
        // ✅ 서비스 계층에서 트림 처리
        String loginId = trim(loginRequest.getLoginId());
        String password = loginRequest.getPassword(); // 비밀번호는 의도된 공백을 허용할 수도 있으니 보통 트림 안함

        MemberInternal member;

        try {
            member = memberServiceClient.verifyPassword(
                    new VerifyPasswordInternal(loginId, password)
            );
        } catch (feign.FeignException e) {
            log.error("[AuthService] 로그인 중 회원 서비스 통신 실패 - loginId: {}, 사유: {}", loginId, e.getMessage());
            throw new FeignException("회원 서비스 통신 실패", e);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("이메일 인증이 필요합니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(member.getId(), member.getLoginId(), member.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(member.getId(), member.getLoginId(), member.getRole().name());

        redisUtil.setDataExpire("RT:" + member.getId(), refreshToken, refreshTokenExpireTimeInSeconds);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        try {
            TokenPayload payload = jwtUtil.parseToken(logoutRequest.getRefreshToken());
            redisUtil.deleteData("RT:" + payload.getId());
        } catch (Exception e) {
            log.error("로그아웃 중 Refresh Token 삭제 실패", e);
            throw new RefreshTokenException("로그아웃 실패: Refresh Token 삭제 중 오류");
        }
    }

    @Override
    public TokenResponse reissue(String refreshToken) {
        TokenPayload payload = jwtUtil.parseToken(refreshToken);
        Long memberId = payload.getId();

        String storedToken = redisUtil.getData("RT:" + memberId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new ForbiddenException("Refresh Token이 유효하지 않습니다.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(memberId, payload.getUsername(), payload.getRole().name());
        return new TokenResponse(newAccessToken, refreshToken);
    }

    @Override
    public boolean checkRefreshTokenStatus(Long memberId) {
        String key = "RT:" + memberId;
        return redisUtil.getData(key) != null;
    }

}