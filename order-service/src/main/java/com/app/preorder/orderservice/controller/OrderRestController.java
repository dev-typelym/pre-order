package com.app.preorder.orderservice.controller;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.orderservice.domain.order.OrderDetailResponse;
import com.app.preorder.orderservice.domain.order.OrderFromCartRequest;
import com.app.preorder.orderservice.domain.order.OrderResponse;
import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderRestController {

    private final OrderService orderService;

    // 단일 상품 주문
    @PostMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Long>> orderItem(
            @PathVariable Long productId,
            @RequestParam Long count) {

        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long memberId = payload.getId();

        Long orderId = orderService.orderSingleItem(memberId, productId, count);
        return ResponseEntity.ok(ApiResponse.success(orderId, "주문이 완료되었습니다."));
    }


    // 카트에서 주문
    @PostMapping("/cart")
    public ResponseEntity<ApiResponse<Long>> orderFromCart(@RequestBody OrderFromCartRequest request) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = payload.getId();

        Long orderId = orderService.orderFromCart(memberId, request.getItems());
        return ResponseEntity.ok(ApiResponse.success(orderId, "장바구니 주문 완료"));
    }

    // 주문 목록
    @GetMapping("/me/orders")
    public Page<OrderResponse> getOrders(@RequestParam(defaultValue = "1") int page) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return orderService.getOrdersWithPaging(page - 1, payload.getId());
    }

    // 주문 상세보기
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(@PathVariable Long orderId) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OrderDetailResponse response = orderService.getOrderDetail(payload.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "주문 상세 조회 성공"));
    }

    // 주문 취소
    @PostMapping("cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@RequestParam Long orderId) {
        orderService.orderCancel(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "주문이 정상적으로 취소되었습니다."));
    }


    // 반품 신청
    @PostMapping("return")
    public ResponseEntity<ApiResponse<Void>> returnOrder(@RequestParam Long orderId) {
        orderService.orderReturn(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "반품 신청이 완료되었습니다."));
    }
}
