package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ProductInboxEventRepository extends JpaRepository<ProductInboxEvent, Long> {

    /** 멱등키 존재 여부(엔티티 로딩 없이 체크) */
    boolean existsByMessageKey(String messageKey);   // ← 이 줄 추가

    /** PENDING 배치 조회(오래된 것부터) */
    List<ProductInboxEvent> findTop100ByStatusOrderByIdAsc(InboxStatus status);
}