package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductInboxEventRepository extends JpaRepository<ProductInboxEvent, Long> {

    /** 멱등키 존재 여부(엔티티 로딩 없이 체크) */
    boolean existsByMessageKey(String messageKey);

    /** (구방식) Top-N 읽기 — 남겨둬도 되고, 이제는 사용하지 않을 예정 */
    List<ProductInboxEvent> findTop100ByStatusOrderByIdAsc(InboxStatus status);

    // ---------- PENDING을 “상태 변경 없이” 행 잠금으로 선점(클레임) ----------

    /**
     * PENDING 행들 중 앞에서부터 LIMIT개 id를 FOR UPDATE로 잠근다.
     * (같은 트랜잭션 내 다른 스레드/인스턴스가 중복 집지 못함)
     */
    @Query(value = """
        SELECT id
          FROM product_inbox_event
         WHERE status = 'PENDING'
         ORDER BY id ASC
         LIMIT :limit
         FOR UPDATE
        """, nativeQuery = true)
    @Transactional // 트랜잭션 안에서 호출되어야 락이 유지된다
    List<Long> lockPendingIds(@Param("limit") int limit);

    /** 잠근 id들만 읽어서 처리 */
    List<ProductInboxEvent> findByIdInOrderByIdAsc(List<Long> ids);
}
