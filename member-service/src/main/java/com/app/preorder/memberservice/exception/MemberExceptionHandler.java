package com.app.preorder.memberservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.memberservice")
public class MemberExceptionHandler {

    // 시스템에서 발생한 Feign 오류
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignSystemException(feign.FeignException ex) {
        log.error("[Member] 시스템 Feign 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "MEMBER_FEIGN_SYSTEM_ERROR"));
    }

    // 커스텀으로 감싼 Feign 오류
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
        log.error("[Member] Feign 통신 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "MEMBER_FEIGN_COMMUNICATION_ERROR"));
    }

    // 비밀번호 불일치
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        log.warn("[Member] 비밀번호 불일치: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "MEMBER_INVALID_PASSWORD"));
    }

    // 사용자 정보를 찾을 수 없음
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("[Member] 사용자 없음: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "MEMBER_USER_NOT_FOUND"));
    }

    // 로그인 자격 증명 실패
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("[Member] 자격 증명 실패: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ex.getMessage(), "MEMBER_INVALID_CREDENTIALS"));
    }

    // 중복된 값 존재 (ex. 아이디, 이메일 등)
    @ExceptionHandler(DuplicateValueException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateValueException ex) {
        log.warn("[Member] 중복 값: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), "MEMBER_DUPLICATE_VALUE"));
    }

    // 이메일 전송 실패
    @ExceptionHandler(EmailSendFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailSendFailed(EmailSendFailedException ex) {
        log.error("[Member] 이메일 전송 실패", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("이메일 전송에 실패했습니다.", "MEMBER_EMAIL_SEND_FAILED"));
    }

    // 알 수 없는 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("[Member] 서버 내부 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("서버 오류 발생", "MEMBER_INTERNAL_SERVER_ERROR"));
    }
}
