package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.request.LoginRequest;
import com.app.preorder.authservice.dto.request.LogoutRequest;
import com.app.preorder.authservice.dto.response.LoginResponse;
import com.app.preorder.authservice.dto.response.TokenResponse;
import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.common.dto.VerifyPasswordInternal;
import com.app.preorder.common.exception.custom.*;
import com.app.preorder.common.type.MemberStatus;
import com.app.preorder.infralib.util.JwtUtil;
import com.app.preorder.infralib.util.RedisUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.List;

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
        String loginId = trim(loginRequest.getLoginId());
        String password = loginRequest.getPassword();

        MemberInternal member;
        try {
            member = memberServiceClient.verifyPassword(new VerifyPasswordInternal(loginId, password));
        } catch (feign.FeignException e) {
            int s = e.status();
            if (s == 401 || s == 404) {
                throw new InvalidCredentialsException("아이디/비밀번호가 올바르지 않습니다.");
            }
            // 나머지는 의존성 장애로 표준화
            log.error("[AuthService] member 호출 실패 status={}, msg={}, body={}",
                    s, e.getMessage(), e.contentUTF8());
            throw new MemberServiceUnavailableException("member-service unavailable", e);
        }

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new ForbiddenException("이메일 인증이 필요합니다.");
        }

        String deviceId = java.util.UUID.randomUUID().toString();
        String accessToken  = jwtUtil.generateAccessToken(member.getId(), member.getLoginId(), member.getRole().name(), deviceId);
        String refreshToken = jwtUtil.generateRefreshToken(member.getId(), member.getLoginId(), member.getRole().name(), deviceId);

        try {
            redisUtil.setDataExpire("RT:" + member.getId() + ":" + deviceId, refreshToken, refreshTokenExpireTimeInSeconds);
            redisUtil.addSet("RTIDX:" + member.getId(), deviceId);
            log.info("Redis 저장 완료: RT:{}:{}, 인덱스 추가", member.getId(), deviceId);
        } catch (Exception ex) {
            log.error("Redis 저장 실패", ex);
        }

        return new LoginResponse(accessToken, refreshToken);
    }

    // ✅ 현재 기기 로그아웃 (본문 RT로 식별)
    @Override
    public void logout(LogoutRequest logoutRequest) {
        try {
            TokenPayload p = jwtUtil.parseToken(logoutRequest.getRefreshToken());
            String rtKey = "RT:" + p.getId() + ":" + p.getDeviceId();
            redisUtil.deleteData(rtKey);
            redisUtil.removeSetMember("RTIDX:" + p.getId(), p.getDeviceId()); // ← 인덱스에서 제거
        } catch (Exception e) {
            log.error("로그아웃 중 Refresh Token 삭제 실패", e);
            throw new RefreshTokenException("로그아웃 실패: Refresh Token 삭제 중 오류");
        }
    }

    // ✅ 전체 로그아웃 — 인덱스 기반으로 모두 삭제
    @Override
    public void logoutAllDevices(Long memberId) {
        Set<String> devices = redisUtil.getSetMembers("RTIDX:" + memberId);
        if (devices != null && !devices.isEmpty()) {
            List<String> keys = devices.stream().map(d -> "RT:" + memberId + ":" + d).toList();
            redisUtil.deleteDataBatch(keys);
        }
        redisUtil.deleteData("RTIDX:" + memberId);
        log.info("✅ 전체 로그아웃 완료 - memberId: {}, deletedDevices: {}", memberId, devices == null ? 0 : devices.size());
    }

    // ✅ 재발급 (기기 단위 키로만 검증)
    @Override
    public TokenResponse reissue(String refreshToken) {
        TokenPayload p = jwtUtil.parseToken(refreshToken);
        Long memberId = p.getId();
        String deviceId = p.getDeviceId();

        String storedToken = redisUtil.getData("RT:" + memberId + ":" + deviceId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new ForbiddenException("Refresh Token이 유효하지 않습니다.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(memberId, p.getUsername(), p.getRole().name(), deviceId);
        return new TokenResponse(newAccessToken, refreshToken);
    }

    // ✅ RT 보유 여부(회원 단위) — 인덱스 존재로 판단
    @Override
    public boolean checkRefreshTokenStatus(Long memberId) {
        Set<String> devs = redisUtil.getSetMembers("RTIDX:" + memberId);
        return devs != null && !devs.isEmpty();
    }
}
