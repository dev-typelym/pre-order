package com.app.preorder.orderservice.repository;


import com.app.preorder.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderQueryDsl {
    // 주문 아이디로 주문 찾기
    public Order findOrderByOrderId_queryDSL(Long orderId);

    //  상품 목록 조회
    Page<Order> findOrdersByMemberId(Long memberId, Pageable pageable);

    //  주문 상세보기
    public Order findOrderItemsByOrderId_queryDSL(Long orderId);
}
