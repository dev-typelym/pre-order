package com.app.preorder.authservice.exception.security;


import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.type.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;


@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");

        String message = "접근가능한 권한을 가지고 있지 않습니다.";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
            if (hasRole(authorities, Role.ROLE_NOT_PERMITTED.name())) {
                message = "사용자 인증메일을 받지 않았습니다.";
            }
        }

        ApiResponse<Void> result = ApiResponse.<Void>builder()
                .success(false)
                .errorCode("ACCESS_DENIED")
                .message(message)
                .data(null)
                .build();

        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(result));
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> authorities, String role) {
        return authorities.contains(new SimpleGrantedAuthority(role));
    }
}