package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.common.type.InboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cart_inbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uq_cart_inbox_message_key", columnNames = "message_key"),
        indexes = @Index(name = "idx_cart_inbox_status_id", columnList = "status,id")
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartInboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="message_key", nullable=false, length=100)
    private String messageKey;

    @Column(name="topic", nullable=false, length=200)
    private String topic;

    @Lob
    @Column(name="payload_json", nullable=false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false, length=30)
    private InboxStatus status;

    @Column(name="error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name="created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    public static CartInboxEvent of(String key, String topic, String json) {
        return CartInboxEvent.builder()
                .messageKey(key).topic(topic).payloadJson(json)
                .status(InboxStatus.PENDING).build();
    }

    public void markProcessed() {
        this.status = InboxStatus.PROCESSED;
        this.errorMessage = null;
    }

    public void markFailed(String reason) {
        this.status = InboxStatus.FAILED;
        this.errorMessage = reason;
    }
}
