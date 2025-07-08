package com.app.preorder.cartservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.CartNotFoundException;
import com.app.preorder.common.exception.custom.FeignException;
import com.app.preorder.common.exception.custom.InvalidCartOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.cartservice")
public class CartExceptionHandler {

    // 커스텀으로 감싼 Feign 오류
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
        log.error("[Cart] 커스텀 Feign 통신 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "CART_FEIGN_COMMUNICATION_ERROR"));
    }

    // 유효하지 않은 장바구니 작업 (예: 수량 0 이하 등)
    @ExceptionHandler(InvalidCartOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCartOperation(InvalidCartOperationException e) {
        log.warn("[Cart] 유효하지 않은 장바구니 작업: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(e.getMessage(), "CART_INVALID_OPERATION"));
    }

    // 장바구니 자체가 존재하지 않음
    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCartNotFound(CartNotFoundException e) {
        log.warn("[Cart] 장바구니를 찾을 수 없습니다: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(e.getMessage(), "CART_NOT_FOUND"));
    }

    // 알 수 없는 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("[Cart] 알 수 없는 서버 내부 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("서버 오류가 발생했습니다.", "CART_INTERNAL_SERVER_ERROR"));
    }
}
