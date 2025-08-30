package com.app.preorder.orderservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.*;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.orderservice")
public class OrderExceptionHandler {

    // 회로 열림(서킷브레이커 차단) → 잠시 후 재시도 유도
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitOpen(CallNotPermittedException ex) {
        log.warn("[Order] Circuit Open (CB 차단)", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.", "ORDER_CIRCUIT_OPEN"));
    }

    // 상품 조회 실패(외부/내부 원인 불문) → 상류 시스템 오류 의미로 502
    @ExceptionHandler(ProductQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductQuery(ProductQueryException ex) {
        log.warn("[Order] 상품 조회 실패", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_PRODUCT_QUERY_FAILED"));
    }

    // 상품 상태 변경(재고 등) 실패(비멱등) → 503로 빠른 실패
    @ExceptionHandler(ProductCommandException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductCommand(ProductCommandException ex) {
        log.error("[Order] 상품 상태 변경(재고) 실패", ex);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_PRODUCT_COMMAND_FAILED"));
    }

    // 회원 조회 실패 → 상류 시스템 오류 의미로 502
    @ExceptionHandler(MemberQueryException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberQuery(MemberQueryException ex) {
        log.warn("[Order] 회원 조회 실패", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_MEMBER_QUERY_FAILED"));
    }

    // 커스텀으로 감싸지 못한 Feign 통신 예외
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

    // 상품 오픈 전 예외
    @ExceptionHandler(ProductNotOpenException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotOpen(ProductNotOpenException ex) {
        log.warn("[Order] 상품 오픈 전 예외", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_PRODUCT_NOT_OPEN"));
    }

    // 상품 판매 종료 예외
    @ExceptionHandler(ProductClosedException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductClosed(ProductClosedException ex) {
        log.warn("[Order] 상품 판매 종료 예외", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_PRODUCT_CLOSED"));
    }

    // 재고 부족 예외
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleStock(InsufficientStockException ex) {
        log.warn("[Order] 재고 부족 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ORDER_STOCK_INSUFFICIENT"));
    }

    // 주문 스케줄 등록 실패
    @ExceptionHandler(OrderScheduleFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrderScheduleFailed(OrderScheduleFailedException ex) {
        log.error("[Order] 주문 스케줄 등록 실패", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("주문 처리 중 스케줄 등록에 실패했습니다.", "ORDER_SCHEDULE_FAILED"));
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
