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
        indexes = @Index(name = "idx_product_outbox_status_id", columnList = "status,id"),
        uniqueConstraints = @UniqueConstraint(name = "uq_product_outbox_message_key", columnNames = "message_key")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOutboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="topic", nullable=false, length=200)
    private String topic;

    @Column(name="partition_key", nullable=false, length=100)
    private String partitionKey;

    @Column(name="message_key", nullable=false, length=100)  // ★ 멱등키
    private String messageKey;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=20)
    private OutboxStatus status;

    @Lob
    @Column(name="payload_json", nullable=false, columnDefinition="LONGTEXT")
    private String payloadJson;

    @Column(name="error_message")
    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 기존 of(...) 대신 messageKey를 받는 팩토리로 변경
    public static ProductOutboxEvent of(String topic, String key, String msgKey, String payloadJson) {
        return ProductOutboxEvent.builder()
                .topic(topic)
                .partitionKey(key)
                .messageKey(msgKey)              // ★ 필수
                .payloadJson(payloadJson)
                .status(OutboxStatus.NEW)
                .build();
    }

    public void markSent() { this.status = OutboxStatus.SENT; this.errorMessage = null; }
    public void markFailed(String reason) { this.status = OutboxStatus.FAILED; this.errorMessage = reason; }
}
