package com.app.preorder.productservice.messaging.inbox;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductInboxEventRepository extends JpaRepository<ProductInboxEvent, Long> {

    // 1) 인박스 적재: UNIQUE(message_key) 기반 멱등 업서트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        INSERT INTO product_inbox_event (message_key, topic, payload_json, status, created_at, updated_at)
        VALUES (:messageKey, :topic, :payloadJson, 'PENDING', NOW(6), NOW(6))
        ON DUPLICATE KEY UPDATE message_key = message_key
        """, nativeQuery = true)
    int upsertPending(@Param("messageKey") String messageKey,
                      @Param("topic") String topic,
                      @Param("payloadJson") String payloadJson);

    // 2) 처리 대상 행 선점(트랜잭션 내 FOR UPDATE로 잠금)
    @Query(value = """
        SELECT id
          FROM product_inbox_event
         WHERE status = 'PENDING'
         ORDER BY id ASC
         LIMIT :limit
         FOR UPDATE
        """, nativeQuery = true)
    @Transactional
    List<Long> lockPendingIds(@Param("limit") int limit);

    // 3) 선점한 id만 로드해서 처리
    List<ProductInboxEvent> findByIdInOrderByIdAsc(List<Long> ids);
}
