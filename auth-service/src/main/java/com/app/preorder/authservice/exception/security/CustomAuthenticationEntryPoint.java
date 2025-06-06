package com.app.preorder.authservice.exception.security;

import com.app.preorder.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 권장
        response.setContentType("application/json;charset=utf-8");

        ApiResponse<Void> result = ApiResponse.<Void>builder()
                .success(false)
                .errorCode("UNAUTHORIZED")
                .message("로그인이 되지 않은 사용자입니다.")
                .data(null)
                .build();

        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(result));
    }
}
