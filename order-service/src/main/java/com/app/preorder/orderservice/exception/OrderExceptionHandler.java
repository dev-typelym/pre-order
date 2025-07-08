package com.app.preorder.orderservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.ForbiddenException;
import com.app.preorder.common.exception.custom.InvalidOrderStatusException;
import com.app.preorder.common.exception.custom.InvalidProductStatusException;
import com.app.preorder.common.exception.custom.OrderNotFoundException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.orderservice")
public class OrderExceptionHandler {

    // 커스텀 Feign 예외
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignCustomException(FeignException ex) {
        log.error("[Order] Feign 통신 오류 발생", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("서비스 간 통신 오류가 발생했습니다.", "ORDER_FEIGN_COMMUNICATION_ERROR"));
    }

    // 주문 없음 예외
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("[Order] 주문 없음 예외", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_NOT_FOUND"));
    }

    // 권한 없음 예외
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("[Order] 권한 없음 예외", ex);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_FORBIDDEN"));
    }


    // 주문 상태 예외
    @ExceptionHandler(InvalidOrderStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidOrderStatus(InvalidOrderStatusException ex) {
        log.warn("[Order] 주문 상태 예외", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_INVALID_STATUS"));
    }

    // 상품 상태 예외
    @ExceptionHandler(InvalidProductStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidProductStatus(InvalidProductStatusException ex) {
        log.warn("[Order] 상품 상태 예외", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_INVALID_PRODUCT_STATUS"));
    }

    // 예상치 못한 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Order] 내부 서버 오류", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("예상치 못한 오류가 발생했습니다.", "ORDER_INTERNAL_SERVER_ERROR"));
    }
}
