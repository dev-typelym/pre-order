package com.app.preorder.authservice.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

    // Access / Refresh 토큰 유효 시간
    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 15; // 15분
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7; // 7일

    @Value("${spring.jwt.secret}")
    private String SECRET_KEY;

    // 서명 키 생성
    private Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // AccessToken 생성
    public String generateToken(Long id, String username) {
        return doGenerateToken(id, username, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // RefreshToken 생성
    public String generateRefreshToken(Long id, String username) {
        return doGenerateToken(id, username, REFRESH_TOKEN_EXPIRE_TIME);
    }

    // 토큰 생성 내부 공통 메서드
    private String doGenerateToken(Long id, String username, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("id", id);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getSigningKey(SECRET_KEY), SignatureAlgorithm.HS256)
                .compact();
    }

}
