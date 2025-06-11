package com.app.preorder.infralib.util;


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

    public static final long ACCESS_TOKEN_EXPIRE_TIME = 1000L * 60 * 15;
    public static final long REFRESH_TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24 * 7;

    @Value("${spring.jwt.secret}")
    private String SECRET_KEY;

    private Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // AccessToken 생성 (role 추가)
    public String generateToken(Long id, String username, Role role) {
        return doGenerateToken(id, username, role, ACCESS_TOKEN_EXPIRE_TIME);
    }

    // RefreshToken 생성 (role 추가)
    public String generateRefreshToken(Long id, String username, Role role) {
        return doGenerateToken(id, username, role, REFRESH_TOKEN_EXPIRE_TIME);
    }

    private String doGenerateToken(Long id, String username, Role role, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("id", id);
        claims.put("username", username);
        claims.put("role", role.name());

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(getSigningKey(SECRET_KEY), SignatureAlgorithm.HS256)
                .compact();
    }
}
