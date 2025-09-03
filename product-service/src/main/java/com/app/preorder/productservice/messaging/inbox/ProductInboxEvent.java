// product-service/src/main/java/com/app/preorder/productservice/messaging/inbox/ProductInboxEvent.java
package com.app.preorder.productservice.messaging.inbox;


import com.app.preorder.common.type.InboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

// product-service/src/main/java/com/app/preorder/productservice/messaging/inbox/ProductInboxEvent.java
// 변경 포인트: lastError 필드명/컬럼명을 errorMessage/error_message 로 교체

@Entity
@Table(
        name = "product_inbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uq_inbox_message_key", columnNames = "message_key"),
        indexes = @Index(name = "idx_product_inbox_status_id", columnList = "status,id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProductInboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboxStatus status;

    @Column(name = "error_message")               // ★ 여기 통일
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ProductInboxEvent of(String messageKey, String topic, String payloadJson) {
        return ProductInboxEvent.builder()
                .messageKey(messageKey)
                .topic(topic)
                .payloadJson(payloadJson)
                .status(InboxStatus.PENDING)
                .build();
    }

    public void markProcessed() { this.status = InboxStatus.PROCESSED; this.errorMessage = null; }
    public void markFailed(String reason) { this.status = InboxStatus.FAILED; this.errorMessage = reason; }
}

