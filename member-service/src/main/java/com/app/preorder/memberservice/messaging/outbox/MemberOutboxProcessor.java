package com.app.preorder.memberservice.messaging.outbox;

import com.app.preorder.common.messaging.command.CartCreateRequest;
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
    private final KafkaTemplate<String, CartCreateRequest> kafkaTemplate;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "${outbox.member.publish-interval-ms:500}")
    @Transactional
    public void flush() {
        List<MemberOutboxEvent> batch = outboxRepo.findTop100ByStatusOrderByIdAsc(OutboxStatus.NEW);
        for (MemberOutboxEvent e : batch) {
            try {
                CartCreateRequest payload = om.readValue(e.getPayloadJson(), CartCreateRequest.class);
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), payload).get();
                e.markSent();
            } catch (Exception ex) {
                log.warn("[Outbox][member] 전송 실패 id={}, topic={}, reason={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed();
            }
        }
    }
}
