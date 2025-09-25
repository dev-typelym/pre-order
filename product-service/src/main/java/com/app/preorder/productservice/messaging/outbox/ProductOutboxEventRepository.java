package com.app.preorder.productservice.messaging.outbox;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductOutboxEventRepository extends JpaRepository<ProductOutboxEvent, Long> {

    /** NEW에서 LIMIT개 id를 FOR UPDATE로 잠가 선점 */
    @Query(value = """
        SELECT id
          FROM product_outbox_event
         WHERE status = 'NEW'
         ORDER BY id ASC
         LIMIT :limit
         FOR UPDATE
        """, nativeQuery = true)
    @Transactional
    List<Long> lockNewIds(@Param("limit") int limit);

    /** 선점된 id들만 읽기 */
    List<ProductOutboxEvent> findByIdInOrderByIdAsc(List<Long> ids);

    /** ✅ 멱등 업서트: 중복 message_key여도 예외 없이 무시 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        INSERT INTO product_outbox_event
            (message_key, topic, partition_key, payload_json, status, created_at, updated_at)
        VALUES
            (:messageKey, :topic, :partitionKey, :payloadJson, 'NEW', NOW(6), NOW(6))
        ON DUPLICATE KEY UPDATE message_key = message_key
        """, nativeQuery = true)
    int upsertNew(@Param("messageKey") String messageKey,
                  @Param("topic") String topic,
                  @Param("partitionKey") String partitionKey,
                  @Param("payloadJson") String payloadJson);
}
