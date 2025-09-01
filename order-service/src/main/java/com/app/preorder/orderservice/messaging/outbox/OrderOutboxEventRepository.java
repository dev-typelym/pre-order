package com.app.preorder.orderservice.messaging.outbox;

import com.app.preorder.common.type.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderOutboxEventRepository extends JpaRepository<OrderOutboxEvent, Long> {
    // PENDING 상위 100건 오래된 것부터
    List<OrderOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}