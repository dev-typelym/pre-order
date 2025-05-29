package com.app.preorder.authservice.filter;

import com.app.preorder.memberservice.entity.Member;
import com.app.preorder.memberservice.service.member.MyUserDetailsService;
import com.app.preorder.memberservice.util.CookieUtil;
import com.app.preorder.memberservice.util.JwtUtil;
import com.app.preorder.memberservice.util.RedisUtil;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public  class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final Cookie jwtToken = cookieUtil.getCookie(request, JwtUtil.ACCESS_TOKEN_NAME);

        String username = null;
        String jwt = null;
        String refreshJwt = null;
        String refreshUname = null;

        try {
            if (jwtToken != null) {
                jwt = jwtToken.getValue();
                username = jwtUtil.getUsername(jwt);
            }
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // ✅ 변경 전: 6줄짜리 인증 처리 코드가 직접 들어있었음
                // ✅ 변경 후: 아래처럼 메서드 호출로 깔끔하게 대체
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    setAuthentication(userDetails, request); // 💡 이 줄이 새로 추가됨
                }
            }
        } catch (ExpiredJwtException e) {
            log.info("AccessToken 만료 → RefreshToken 인증 루틴 진입");
            Cookie refreshToken = cookieUtil.getCookie(request, JwtUtil.REFRESH_TOKEN_NAME);
            if (refreshToken != null) {
                refreshJwt = refreshToken.getValue();
            }
        } catch (Exception e) {
            log.error("JWT 처리 중 예상치 못한 예외 발생", e);
        }

        try {
            if (refreshJwt != null) {
                refreshUname = redisUtil.getData(refreshJwt);
                if (refreshUname.equals(jwtUtil.getUsername(refreshJwt))) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(refreshUname);

                    // ✅ 이 부분도 동일하게 인증 처리 코드 → 메서드 호출로 변경
                    setAuthentication(userDetails, request); // 💡 이 줄이 새로 추가됨

                    Member member = Member.builder()
                            .username(refreshUname)
                            .build();

                    String newToken = jwtUtil.generateToken(member);
                    Cookie newAccessToken = cookieUtil.createCookie(JwtUtil.ACCESS_TOKEN_NAME, newToken);
                    response.addCookie(newAccessToken);
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("RefreshToken 만료 → 재로그인 필요");
        }

        filterChain.doFilter(request, response);
    }

    // ✅ 추가된 메서드 시작
    // 💡 인증 설정 로직을 이 메서드로 모아서 중복 제거
    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    // ✅ 추가된 메서드 끝
}
