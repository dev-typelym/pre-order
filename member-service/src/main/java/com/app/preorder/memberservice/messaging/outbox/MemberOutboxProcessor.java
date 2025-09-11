package com.app.preorder.memberservice.messaging.outbox;

import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.event.MemberDeactivatedEvent;   // ✅ 추가
import com.app.preorder.common.messaging.topics.KafkaTopics;            // ✅ 추가
import com.app.preorder.common.type.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberOutboxProcessor {

    private final MemberOutboxEventRepository outboxRepo;
    private final ObjectMapper om;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.member.publish-interval-ms:700}") // 필요하면 500ms로 그대로 둬도 됨
    @Transactional
    public void flush() {
        List<MemberOutboxEvent> batch = outboxRepo.findTop100ByStatusOrderByIdAsc(OutboxStatus.NEW);

        for (MemberOutboxEvent e : batch) {
            try {
                Object payload = switch (e.getTopic()) {
                    case KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1 -> om.readValue(e.getPayloadJson(), CartCreateRequest.class);
                    case KafkaTopics.MEMBER_DEACTIVATED_V1         -> om.readValue(e.getPayloadJson(), MemberDeactivatedEvent.class);
                    default -> throw new IllegalArgumentException("지원하지 않는 토픽: " + e.getTopic());
                };

                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), payload).get();
                e.markSent();

            } catch (Exception ex) {
                log.warn("[Outbox][member] 전송 실패 id={}, topic={}, reason={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }
}
