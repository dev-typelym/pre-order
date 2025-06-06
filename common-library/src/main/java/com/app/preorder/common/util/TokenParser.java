package com.app.preorder.common.util;

import com.app.preorder.common.dto.TokenPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
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

        return new TokenPayload(id, username);
    }
}