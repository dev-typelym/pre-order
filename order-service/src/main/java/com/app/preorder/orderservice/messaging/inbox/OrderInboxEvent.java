package com.app.preorder.orderservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_inbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_inbox_message_key", columnNames = "message_key"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderInboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey; // eventId

    @Column(nullable = false, length = 200)
    private String topic;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboxStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public static OrderInboxEvent of(String key, String topic, String json) {
        return OrderInboxEvent.builder()
                .messageKey(key)
                .topic(topic)
                .payloadJson(json)
                .status(InboxStatus.PENDING)
                .build();
    }

    public void markProcessed() { this.status = InboxStatus.PROCESSED; this.errorMessage = null; }
    public void markFailed(String reason) { this.status = InboxStatus.FAILED; this.errorMessage = reason; }
}

