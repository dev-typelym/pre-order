package com.app.preorder.productservice.messaging.outbox;

import com.app.preorder.common.type.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_outbox_event",
        indexes = @Index(name = "idx_product_outbox_status_id", columnList = "status,id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductOutboxEvent {

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
    private OutboxStatus status; // NEW / SENT / FAILED

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 팩토리: 기본값/규칙 강제
    public static ProductOutboxEvent of(String topic, String key, String payloadJson) {
        return ProductOutboxEvent.builder()
                .topic(topic)
                .partitionKey(key)
                .payloadJson(payloadJson)
                .status(OutboxStatus.NEW)
                .build();
    }

    public void markSent()   { this.status = OutboxStatus.SENT;   this.errorMessage = null; }
    public void markFailed(String reason) { this.status = OutboxStatus.FAILED; this.errorMessage = reason; }
}
