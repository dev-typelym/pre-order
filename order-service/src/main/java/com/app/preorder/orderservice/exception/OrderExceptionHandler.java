package com.app.preorder.orderservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.*;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.orderservice")
public class OrderExceptionHandler {

    // 서킷 오픈 → 503
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitOpen(CallNotPermittedException ex) {
        log.warn("[Order] Circuit Open (CB 차단)", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.", "ORDER_CIRCUIT_OPEN"));
    }

    // 상류(product/member)에서 온 상태코드 그대로 전달 (409/404/503 등 유지)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeign(FeignException ex) {
        log.warn("[Order] Upstream(feign) status={}, msg={}", ex.status(), ex.getMessage());
        HttpStatus st = HttpStatus.resolve(ex.status());
        if (st == null) st = HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(st)
                .body(ApiResponse.failure(ex.getMessage(), "UPSTREAM_ERROR"));
    }

    // 404
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderNotFound(OrderNotFoundException ex) {
        log.warn("[Order] 주문 없음", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_NOT_FOUND"));
    }

    // 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        log.warn("[Order] 권한 없음", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_FORBIDDEN"));
    }

    // ✅ 409: 도메인/상태 충돌(전이 불가/만료/재고/상품 상태 등) + 제약 충돌
    @ExceptionHandler({
            InvalidOrderStatusException.class,
            InvalidProductStatusException.class,
            ProductNotOpenException.class,
            ProductClosedException.class,
            InsufficientStockException.class,
            DataIntegrityViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleOrderConflict(Exception ex) {
        log.warn("[Order] 상태/경쟁 충돌", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_CONFLICT"));
    }

    // 502/503: 조회/커맨드 실패(네가 이미 쓰는 커스텀 예외 유지)
    @ExceptionHandler(ProductQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductQuery(ProductQueryException ex) {
        log.warn("[Order] 상품 조회 실패", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_PRODUCT_QUERY_FAILED"));
    }

    @ExceptionHandler(ProductCommandException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductCommand(ProductCommandException ex) {
        log.error("[Order] 상품 상태 변경(재고) 실패", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_PRODUCT_COMMAND_FAILED"));
    }

    // 500: 그 외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Order] 내부 서버 오류", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("예상치 못한 오류가 발생했습니다.", "ORDER_INTERNAL_SERVER_ERROR"));
    }
}
