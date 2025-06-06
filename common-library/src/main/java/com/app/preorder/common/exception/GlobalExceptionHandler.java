package com.app.preorder.common.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.exception.custom.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
        log.error("Feign 통신 오류 발생", ex);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .errorCode("FEIGN_COMMUNICATION_ERROR")
                        .message("서비스 간 통신 오류가 발생했습니다.")
                        .data(null)
                        .build());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .errorCode("USER_NOT_FOUND")
                        .message(ex.getMessage())
                        .data(null)
                        .build());
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .message("서버 오류 발생")
                        .data(null)
                        .build());
    }
}