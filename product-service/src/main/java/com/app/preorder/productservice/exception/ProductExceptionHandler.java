package com.app.preorder.productservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.common.exception.custom.ProductAlreadyHasStockException;
import com.app.preorder.common.exception.custom.ProductNotFoundException;
import com.app.preorder.common.exception.custom.StockNotFoundException;
import com.app.preorder.common.exception.custom.InvalidStockRequestException;
import com.app.preorder.common.exception.custom.UnreserveFailedException;
import com.app.preorder.common.exception.custom.RestockFailedException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.productservice")
public class ProductExceptionHandler {

    // 상품을 찾을 수 없음 → 404
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex) {
        log.warn("[Product] 상품 없음 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_NOT_FOUND"));
    }

    // 재고를 찾을 수 없음 → 404
    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockNotFound(StockNotFoundException ex) {
        log.warn("[Product] 재고 없음 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_NOT_FOUND"));
    }

    // 재고 부족(예약/커밋 실패 등) → 400
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleStock(InsufficientStockException ex) {
        log.warn("[Product] 재고 부족 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_INSUFFICIENT"));
    }

    // 잘못된 재고 요청(빈 목록 등) → 400
    @ExceptionHandler(InvalidStockRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStockRequest(InvalidStockRequestException ex) {
        log.warn("[Product] 잘못된 재고 요청", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_INVALID_REQUEST"));
    }

    // 예약 해제 실패 → 400
    @ExceptionHandler(UnreserveFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreserveFailed(UnreserveFailedException ex) {
        log.warn("[Product] 예약 해제 실패", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_UNRESERVE_FAILED"));
    }

    // 재입고(보상) 실패 → 400
    @ExceptionHandler(RestockFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestockFailed(RestockFailedException ex) {
        log.warn("[Product] 재입고 실패", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_RESTORE_FAILED"));
    }

    // 상품에 이미 재고가 연결된 경우(1:1 규칙 위반) → 409
    @ExceptionHandler(ProductAlreadyHasStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductAlreadyHasStock(ProductAlreadyHasStockException ex) {
        log.warn("[Product] 재고 중복 연결 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_ALREADY_HAS_STOCK"));
    }

    // 일반 시스템 예외 → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Product] 예상치 못한 서버 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("예상치 못한 오류가 발생했습니다.", "PRODUCT_INTERNAL_SERVER_ERROR"));
    }
}
