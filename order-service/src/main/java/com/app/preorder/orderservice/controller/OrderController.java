package com.app.preorder.orderservice.controller;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.orderservice.domain.order.OrderDetailResponse;
import com.app.preorder.orderservice.domain.order.OrderFromCartRequest;
import com.app.preorder.orderservice.domain.order.OrderResponse;
import com.app.preorder.orderservice.domain.order.UpdateOrderAddressRequest;
import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    // 단일 상품 주문
    @PostMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Long>> orderItem(
            @PathVariable Long productId,
            @RequestParam Long count) {

        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long memberId = payload.getId();

        Long orderId = orderService.prepareSingleOrder(memberId, productId, count);
        return ResponseEntity.ok(ApiResponse.success(orderId, "주문이 완료되었습니다."));
    }


    // 카트에서 주문
    @PostMapping("/cart")
    public ResponseEntity<ApiResponse<Long>> orderFromCart(@RequestBody OrderFromCartRequest request) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = payload.getId();

        Long orderId = orderService.prepareCartOrder(memberId, request.getItems());
        return ResponseEntity.ok(ApiResponse.success(orderId, "장바구니 주문 완료"));
    }

    // 결제 시도
    @PostMapping("/{orderId}/attempt")
    public ResponseEntity<ApiResponse<Void>> attemptPayment(@PathVariable Long orderId) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        orderService.attemptPayment(orderId, payload.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "결제 시도가 완료되었습니다."));
    }

    // 결제 완료
    @PostMapping("/{orderId}/complete")
    public ResponseEntity<ApiResponse<Void>> completePayment(@PathVariable Long orderId) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        orderService.completePayment(orderId, payload.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "결제가 완료되었습니다."));
    }

    // 주문 목록
    @GetMapping("/me/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<OrderResponse> result = orderService.getOrdersWithPaging(page - 1, size, payload.getId());
        return ResponseEntity.ok(ApiResponse.success(result, "주문 목록 조회 성공"));
    }

    // 주문 상세보기
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(@PathVariable Long orderId) {
        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        OrderDetailResponse response = orderService.getOrderDetail(payload.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "주문 상세 조회 성공"));
    }

    //  주문 수정
    @PatchMapping("/{orderId}/address")
    public ResponseEntity<ApiResponse<Void>> updateOrderAddress(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderAddressRequest request
    ) {
        orderService.updateOrderAddress(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "배송지가 성공적으로 변경되었습니다."));
    }

    // 주문 취소
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long orderId) {
        orderService.orderCancel(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "주문이 정상적으로 취소되었습니다."));
    }


    // 반품 신청
    @PostMapping("/{orderId}/return")
    public ResponseEntity<ApiResponse<Void>> returnOrder(@PathVariable Long orderId) {
        orderService.orderReturn(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "반품 신청이 완료되었습니다."));
    }
}
