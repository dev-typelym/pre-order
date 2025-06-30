package com.app.preorder.orderservice.repository;


import com.app.preorder.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderQueryDsl {

    //  상품 목록 조회
    Page<Order> findOrdersByMemberId(Long memberId, Pageable pageable);

}
