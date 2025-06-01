package com.app.preorder.authservice.exception;

import com.app.preorder.common.util.Response;
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
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        httpServletResponse.setStatus(200);
        httpServletResponse.setContentType("application/json;charset=utf-8");
        Response response = new Response("error","로그인이 되지 않은 사용자입니다.",null);
        PrintWriter out = httpServletResponse.getWriter();
        String jsonResponse = objectMapper.writeValueAsString(response);
        out.print(jsonResponse);
    }

}
