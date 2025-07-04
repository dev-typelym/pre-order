package com.app.preorder.common.util;

import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.common.type.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;


public class TokenParser {

    private final Key key;

    public TokenParser(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenPayload parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(this.key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long id = claims.get("id", Long.class);
        String username = claims.get("username", String.class);
        String roleStr = claims.get("role", String.class);
        Role role = Role.valueOf(roleStr);  // enum으로 변환

        return new TokenPayload(id, username, role);
    }
}