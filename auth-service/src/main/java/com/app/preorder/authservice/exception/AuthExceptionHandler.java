package com.app.preorder.authservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.authservice")
public class AuthExceptionHandler {

    // 커스텀으로 감싼 Feign 오류
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignCustomException(FeignException ex) {
        log.error("[Auth] Feign 통신 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "AUTH_FEIGN_COMMUNICATION_ERROR"));
    }

    // 로그인 자격 증명 실패 (아이디/비밀번호 불일치)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("[Auth] 자격 증명 실패: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure("아이디/비밀번호가 올바르지 않습니다.", "AUTH_INVALID_CREDENTIALS"));
    }

    // 멤버서비스 의존성 장애/지연 (타임아웃·5xx 등)
    @ExceptionHandler(MemberServiceUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberUnavailable(MemberServiceUnavailableException ex) {
        log.error("[Auth] member-service 의존성 장애: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("로그인 서비스가 일시적으로 지연 중입니다.", "AUTH_MEMBER_UNAVAILABLE"));
    }

    // 사용자 정보를 찾을 수 없음
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("[Auth] 사용자 없음: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "AUTH_USER_NOT_FOUND"));
    }

    // 인증된 사용자가 접근 권한 없음
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("[Auth] 접근 거부: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), "AUTH_FORBIDDEN"));
    }

    // 리프레시 토큰 예외 처리
    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenException(RefreshTokenException ex) {
        log.error("[Auth] 로그아웃 처리 실패: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ex.getMessage(), "AUTH_LOGOUT_ERROR"));
    }

    // 알 수 없는 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("[Auth] 서버 내부 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("서버 오류 발생", "AUTH_INTERNAL_SERVER_ERROR"));
    }
}
