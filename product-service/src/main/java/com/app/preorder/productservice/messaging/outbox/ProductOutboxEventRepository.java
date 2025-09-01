package com.app.preorder.productservice.messaging.outbox;

import com.app.preorder.common.type.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOutboxEventRepository extends JpaRepository<ProductOutboxEvent, Long> {
    List<ProductOutboxEvent> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
}
