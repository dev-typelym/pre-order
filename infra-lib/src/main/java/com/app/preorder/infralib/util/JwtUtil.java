package com.app.preorder.infralib.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private final String jwtSecret;

    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 15;
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7;

    public JwtUtil(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ✅ 변경된 부분: role을 String으로 받음
    public String generateToken(Long id, String username, String role) {
        return doGenerateToken(id, username, role, ACCESS_TOKEN_EXPIRE_TIME);
    }

    public String generateRefreshToken(Long id, String username, String role) {
        return doGenerateToken(id, username, role, REFRESH_TOKEN_EXPIRE_TIME);
    }

    private String doGenerateToken(Long id, String username, String role, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("id", id);
        claims.put("username", username);
        claims.put("role", role);  // 이미 문자열로 받음

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}