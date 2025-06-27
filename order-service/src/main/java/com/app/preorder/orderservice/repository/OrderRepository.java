package com.app.preorder.orderservice.repository;


import com.app.preorder.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderQueryDsl {
}
