package com.app.preorder.repository.order;

import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderQueryDsl {

    //  상품 목록 조회
    public Page<Order> findAllOrder_queryDSL(Pageable pageable, Long memberId);

    //  주문 상세보기
    public Order findOrderItemsByOrderId_queryDSL(Long orderId);
}
