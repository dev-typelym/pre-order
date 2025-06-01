package com.app.preorder.authservice.config;

import com.app.preorder.authservice.exception.CustomAccessDeniedHandler;
import com.app.preorder.authservice.exception.CustomAuthenticationEntryPoint;
import com.app.preorder.authservice.filter.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

//    private static final String ADMIN_PATH = "/admin/**";
//    private static final String MYPAGE_PATH = "/mypage/**";
//    private static final String LOGIN_PAGE = "/member/login";
//    private static final String LOGIN_PROCESS = "/member/loginProcess";
//    private static final String SIGNUP_PAGE = "/member/signup";
//    private static final String SIGNUP_PROCESS = "/member/join";
//    private static final String EMAIL_VERIFY = "/members/verify/**";

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())  // 폼 로그인 명시적으로 비활성화
                .httpBasic(httpBasic -> httpBasic.disable()) // httpBasic 명시적으로 비활성화
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
