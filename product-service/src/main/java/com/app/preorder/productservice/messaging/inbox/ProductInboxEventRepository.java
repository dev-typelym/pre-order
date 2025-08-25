package com.app.preorder.productservice.messaging.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductInboxEventRepository extends JpaRepository<ProductInboxEvent, Long> {
    // 한 줄 주석: 멱등키로 단건 조회
    Optional<ProductInboxEvent> findByMessageKey(String messageKey);

    // 한 줄 주석: PENDING 배치 조회(오래된 것부터)
    List<ProductInboxEvent> findTop100ByStatusOrderByCreatedAtAsc(ProductInboxStatus status);
}
