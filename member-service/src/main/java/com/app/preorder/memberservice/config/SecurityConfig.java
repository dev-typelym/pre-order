package com.app.preorder.memberservice.config;
import com.app.preorder.memberservice.service.member.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    private static final String ADMIN_PATH = "/admin/**";
    private static final String MYPAGE_PATH = "/mypage/**";
    private static final String LOGIN_PAGE = "/member/login";
    private static final String LOGIN_PROCESS = "/member/loginProcess";
    private static final String SIGNUP_PAGE = "/member/signup";
    private static final String SIGNUP_PROCESS = "/member/join";
    private static final String EMAIL_VERIFY = "/members/verify/**";
    // 대칭키
    @Value("${spring.symmetric.key}")
    private String symmetrickey;


    // AesBytesEncryptor 사용을 위한 Bean등록
    @Bean
    public AesBytesEncryptor aesBytesEncryptor() {
        return new AesBytesEncryptor(symmetrickey,"70726574657374");
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
//        WebSecurity에서 관여하지 않을 경로
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()); //static 경로도 필터에서 제외
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {


         http.csrf(csrf -> csrf.disable())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .httpBasic()
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .and()
                .exceptionHandling().accessDeniedHandler(customAccessDeniedHandler)
                .and()
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(SIGNUP_PAGE).permitAll()
                        .requestMatchers(SIGNUP_PROCESS).permitAll()
                        .requestMatchers(LOGIN_PAGE).permitAll()
                        .requestMatchers(LOGIN_PROCESS).permitAll()
                        .requestMatchers(EMAIL_VERIFY).permitAll()
                .anyRequest().authenticated());

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

//    @Override // ignore check swagger resource
//    public void configure(WebSecurity web) {
//        web.ignoring().requestMatchers("/v2/api-docs", "/swagger-resources/**",
//                "/swagger-ui.html", "/webjars/**", "/swagger/**");
//    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .build();
    }
}
