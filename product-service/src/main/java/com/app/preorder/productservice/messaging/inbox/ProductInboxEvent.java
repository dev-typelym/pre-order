package com.app.preorder.productservice.messaging.inbox;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "product_inbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uq_inbox_message_key", columnNames = "message_key"))
@Getter @Setter @NoArgsConstructor
public class ProductInboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;               // 이벤트 id 등

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductInboxStatus status = ProductInboxStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public static ProductInboxEvent of(String messageKey, String topic, String payloadJson) {
        ProductInboxEvent e = new ProductInboxEvent();
        e.setMessageKey(messageKey);
        e.setTopic(topic);
        e.setPayloadJson(payloadJson);
        e.setStatus(ProductInboxStatus.PENDING);
        return e;
    }

    public void markProcessed() {
        this.status = ProductInboxStatus.PROCESSED;
    }
}
