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
}
