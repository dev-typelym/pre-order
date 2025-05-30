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

    // Access / Refresh 토큰 유효 시간 (예시값)
    public static final long TOKEN_VALIDATION_SECOND = 1000L * 10; // 10초
    public static final long REFRESH_TOKEN_VALIDATION_SECOND = 1000L * 60 * 60 * 24 * 2; // 2일

    public static final String ACCESS_TOKEN_NAME = "accessToken";
    public static final String REFRESH_TOKEN_NAME = "refreshToken";

    @Value("${spring.jwt.secret}")
    private String SECRET_KEY;

    // 서명 키 생성
    private Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // AccessToken 생성
    public String generateToken(String username) {
        return doGenerateToken(username, TOKEN_VALIDATION_SECOND);
    }

    // RefreshToken 생성
    public String generateRefreshToken(String username) {
        return doGenerateToken(username, REFRESH_TOKEN_VALIDATION_SECOND);
    }

    // 토큰 생성 내부 공통 메서드
    private String doGenerateToken(String username, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getSigningKey(SECRET_KEY), SignatureAlgorithm.HS256)
                .compact();
    }

    // Claims 추출
    public Claims extractAllClaims(String token) throws ExpiredJwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(SECRET_KEY))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // username 추출
    public String getUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        final Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    // 단순 토큰 유효성 검증 (UserDetails 의존 제거)
    public boolean validateToken(String token, String username) {
        final String extractedUsername = getUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}