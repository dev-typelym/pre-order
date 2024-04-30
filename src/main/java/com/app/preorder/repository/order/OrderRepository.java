package com.app.preorder.repository.order;

import com.app.preorder.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderQueryDsl {
}
