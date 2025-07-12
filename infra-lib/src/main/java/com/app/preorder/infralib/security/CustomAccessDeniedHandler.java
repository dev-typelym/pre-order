package com.app.preorder.infralib.security;


import com.app.preorder.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import java.io.IOException;


@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");
        ApiResponse<Void> result = ApiResponse.failure("권한이 없습니다.", "ACCESS_DENIED");
        response.getWriter().print(new ObjectMapper().writeValueAsString(result));
    }
}