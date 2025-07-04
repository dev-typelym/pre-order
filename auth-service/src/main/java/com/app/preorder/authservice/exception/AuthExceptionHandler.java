package com.app.preorder.authservice.exception;

import com.app.preorder.common.exception.custom.ForbiddenException;
import com.app.preorder.common.exception.custom.RefreshTokenException;
import com.app.preorder.common.exception.custom.UserNotFoundException;
import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.authservice")
public class AuthExceptionHandler {

    // 시스템에서 발생한 Feign 오류
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignSystemException(feign.FeignException ex) {
        log.error("[Auth] 시스템 Feign 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "AUTH_FEIGN_SYSTEM_ERROR"));
    }

    // 커스텀으로 감싼 Feign 오류
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignCustomException(FeignException ex) {
        log.error("[Auth] Feign 통신 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "AUTH_FEIGN_COMMUNICATION_ERROR"));
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