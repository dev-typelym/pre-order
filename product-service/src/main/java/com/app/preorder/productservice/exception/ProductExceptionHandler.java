package com.app.preorder.productservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.productservice")
public class ProductExceptionHandler {

    /* 404: 리소스 없음 */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex) {
        log.warn("[Product] 상품 없음", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_NOT_FOUND"));
    }

    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockNotFound(StockNotFoundException ex) {
        log.warn("[Product] 재고 없음", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_NOT_FOUND"));
    }

    /* 409: 경쟁/도메인 상태 충돌 (요청은 형식 OK, 현재 상태 때문에 불가) */
    @ExceptionHandler({
            InsufficientStockException.class,      // 재고 부족(예약/커밋 경쟁 실패)
            UnreserveFailedException.class,        // 예약 해제 불가(이미 해제/수량 모순 등)
            RestockFailedException.class,          // 보상(재입고) 불가
            ProductAlreadyHasStockException.class  // 재고-상품 1:1 불변식 위반
    })
    public ResponseEntity<ApiResponse<Void>> handleBusinessConflict(RuntimeException ex) {
        log.warn("[Product] 비즈니스 충돌(409)", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_CONFLICT"));
    }

    /* 409: 동시성/제약 충돌 (유니크키, 락, 낙관적락 등) */
    @ExceptionHandler({
            DataIntegrityViolationException.class,
            OptimisticLockingFailureException.class,
            CannotAcquireLockException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleDbConflict(Exception ex) {
        log.warn("[Product] DB/동시성 충돌(409)", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("data/concurrency conflict", "DB_CONFLICT"));
    }

    /* 400: 입력/파싱/검증 오류 */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class,
            InvalidStockRequestException.class   // ★ 여기로 이동
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        log.warn("[Product] 잘못된 요청(400)", ex);
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ex.getMessage(), "BAD_REQUEST"));
    }

    /* 500: 나머지 예기치 못한 오류 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Product] 내부 오류(500)", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("예상치 못한 오류가 발생했습니다.", "PRODUCT_INTERNAL_SERVER_ERROR"));
    }
}
