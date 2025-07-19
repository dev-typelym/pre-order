package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.PendingQuantityInternal;
import com.app.preorder.orderservice.domain.order.OrderDetailResponse;
import com.app.preorder.orderservice.domain.order.OrderItemRequest;
import com.app.preorder.orderservice.domain.order.OrderResponse;
import com.app.preorder.orderservice.domain.order.UpdateOrderAddressRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {

    List<PendingQuantityInternal> getPendingQuantities(List<Long> productIds);

    // 단건 주문
    Long prepareSingleOrder(Long memberId, Long productId, Long quantity);

    // 카트 다건 주문
    Long prepareCartOrder(Long memberId, List<OrderItemRequest> items);

    //
    void attemptPayment(Long orderId, Long memberId);

    //
    void completePayment(Long orderId, Long memberId);

    // 주문 목록 조회
    Page<OrderResponse> getOrdersWithPaging(int page, int size, Long memberId);

    // 주문 상세보기
    OrderDetailResponse getOrderDetail(Long memberId, Long orderId);

    // 배송지 수정
    void updateOrderAddress(Long orderId, UpdateOrderAddressRequest request);

    // 주문 취소
    void orderCancel(Long orderId);

    // 반품 신청
    void orderReturn(Long orderId);

    // 주문 상태를 "배송 중"으로 업데이트
    void updateOrderStatusShipping(Long orderId);

    // 주문 상태를 "배송 완료"로 업데이트
    void updateOrderStatusDelivered(Long orderId);

    // 주문 상태를 "반품 불가"로 업데이트
    void updateOrderStatusNonReturnable(Long orderId);

    // 반품 처리 (재고 복원 포함)
    void processReturn(Long orderId);
}
