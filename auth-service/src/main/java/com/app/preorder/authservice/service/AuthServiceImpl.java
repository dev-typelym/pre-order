package com.app.preorder.authservice.service;

import com.app.preorder.authservice.client.MemberServiceClient;
import com.app.preorder.authservice.dto.VerifyPasswordRequest;
import com.app.preorder.authservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberServiceClient memberServiceClient;
    private final JwtUtil jwtUtil;

    @Override
    public String login(String username, String password) {
        VerifyPasswordRequest request = new VerifyPasswordRequest();
        request.setUsername(username);
        request.setPassword(password);

        boolean isValid = memberServiceClient.verifyPassword(request);
        if (!isValid) {
            throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return jwtUtil.generateToken(username);
    }
}