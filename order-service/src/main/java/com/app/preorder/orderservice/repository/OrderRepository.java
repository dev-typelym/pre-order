package com.app.preorder.orderservice.repository;


import com.app.preorder.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderQueryDsl {

    //  주문아이디로 주문찾기
    Optional<Order> findOrderById(Long orderId);
}
