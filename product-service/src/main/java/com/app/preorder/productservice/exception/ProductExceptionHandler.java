package com.app.preorder.productservice.exception;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.common.exception.custom.ProductNotFoundException;
import com.app.preorder.common.exception.custom.StockNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.app.preorder.productservice")
public class ProductExceptionHandler {

    // 상품을 찾을 수 없음
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotFound(ProductNotFoundException ex) {
        log.warn("[Product] 상품 없음 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_NOT_FOUND"));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleStock(InsufficientStockException ex) {
        log.warn("[Product] 재고 부족 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_INSUFFICIENT"));
    }

    // 재고를 찾을 수 없음
    @ExceptionHandler(StockNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockNotFound(StockNotFoundException ex) {
        log.warn("[Product] 재고 없음 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage(), "PRODUCT_STOCK_NOT_FOUND"));
    }

    // 일반 시스템 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("[Product] 예상치 못한 서버 예외 발생", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("예상치 못한 오류가 발생했습니다.", "PRODUCT_INTERNAL_SERVER_ERROR"));
    }
}
