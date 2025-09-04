package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartInboxEventRepository extends JpaRepository<CartInboxEvent, Long> {
    boolean existsByMessageKey(String key);
    List<CartInboxEvent> findTop100ByStatusOrderByIdAsc(InboxStatus status);
}
