package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ProductInboxEventRepository extends JpaRepository<ProductInboxEvent, Long> {

    /** 멱등키(eventId 등)로 단건 조회 */
    Optional<ProductInboxEvent> findByMessageKey(String messageKey);

    /** PENDING 배치 조회(오래된 것부터) */
    List<ProductInboxEvent> findTop100ByStatusOrderByIdAsc(InboxStatus status);
}