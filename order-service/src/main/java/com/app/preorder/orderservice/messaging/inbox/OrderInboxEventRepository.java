package com.app.preorder.orderservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderInboxEventRepository extends JpaRepository<OrderInboxEvent, Long> {
    boolean existsByMessageKey(String messageKey);
    List<OrderInboxEvent> findTop100ByStatusOrderByIdAsc(InboxStatus status);
}