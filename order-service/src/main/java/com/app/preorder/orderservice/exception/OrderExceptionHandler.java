package com.app.preorder.orderservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.orderservice")
public class OrderExceptionHandler {

    // 시스템에서 발생한 Feign 오류
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignSystemException(feign.FeignException ex) {
        log.error("[Order] 시스템 Feign 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "ORDER_FEIGN_SYSTEM_ERROR"));
    }

    // 커스텀으로 감싼 Feign 오류
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignCustomException(FeignException ex) {
        log.error("[Order] Feign 통신 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "ORDER_FEIGN_COMMUNICATION_ERROR"));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleStock(InsufficientStockException ex) {
        log.warn("[Order] 재고 부족 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_STOCK_INSUFFICIENT"));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(OrderNotFoundException ex) {
        log.warn("[Order] 주문 없음 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_NOT_FOUND"));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidState(InvalidOrderStateException ex) {
        log.warn("[Order] 잘못된 주문 상태", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_INVALID_STATE"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Order] 알 수 없는 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("예상치 못한 오류가 발생했습니다.", "ORDER_INTERNAL_SERVER_ERROR"));
    }
}
