package com.app.preorder.infralib.util;


import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.common.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String jwtSecret;

    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24;
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ✅ Access Token 생성
    public String generateAccessToken(Long id, String username, String role, String deviceId) {
        return doGenerateToken(id, username, role, deviceId, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // ✅ Refresh Token 생성
    public String generateRefreshToken(Long id, String username, String role, String deviceId) {
        return doGenerateToken(id, username, role, deviceId, REFRESH_TOKEN_EXPIRE_TIME);
    }

    // ✅ 실제 JWT 생성 로직
    private String doGenerateToken(Long id, String username, String role, String deviceId, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("id", id);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("deviceId", deviceId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Token 파싱 및 검증
    public TokenPayload parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long id = Long.valueOf(claims.get("id", Long.class));
        String username = claims.get("username", String.class);
        Role role = Role.valueOf(claims.get("role", String.class));
        String deviceId = claims.get("deviceId", String.class);

        return new TokenPayload(id, username, role, deviceId);
    }
}