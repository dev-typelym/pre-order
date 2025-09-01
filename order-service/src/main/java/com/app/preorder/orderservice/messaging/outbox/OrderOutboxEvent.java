package com.app.preorder.orderservice.messaging.outbox;

import com.app.preorder.common.type.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_outbox_event",
        indexes = @Index(name = "idx_order_outbox_status_id", columnList = "status,id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class OrderOutboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "partition_key", nullable = false, length = 128)
    private String partitionKey;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status; // NEW/SENT/FAILED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 기본값/불변식 강제: status=NEW */
    public static OrderOutboxEvent of(String topic, String partitionKey, String payloadJson) {
        return OrderOutboxEvent.builder()
                .topic(topic)
                .partitionKey(partitionKey)
                .payloadJson(payloadJson)
                .status(OutboxStatus.NEW)
                .build();
    }


    // 상태 전이 도메인 메서드
    public void markSent()   { this.status = OutboxStatus.SENT; }
    public void markFailed() { this.status = OutboxStatus.FAILED; }
}
