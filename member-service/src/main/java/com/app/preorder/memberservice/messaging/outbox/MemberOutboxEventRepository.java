package com.app.preorder.memberservice.messaging.outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemberOutboxEventRepository extends JpaRepository<MemberOutboxEvent, Long> {
    List<MemberOutboxEvent> findTop100ByStatusOrderByIdAsc(MemberOutboxEvent.Status status);
}
