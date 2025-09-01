// member-service/.../messaging/outbox/MemberOutboxEvent.java
package com.app.preorder.memberservice.messaging.outbox;

import com.app.preorder.common.type.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_outbox_event",
        indexes = @Index(name = "idx_member_outbox_status_id", columnList = "status,id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberOutboxEvent {

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
    private OutboxStatus status; // ★ 공통 OutboxStatus: NEW/SENT/FAILED

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 생성 시 기본값 NEW로 세팅할 수 있게 팩토리 제공(권장) */
    public static MemberOutboxEvent of(String topic, String partitionKey, String payloadJson) {
        return MemberOutboxEvent.builder()
                .topic(topic)
                .partitionKey(partitionKey)
                .payloadJson(payloadJson)
                .status(OutboxStatus.NEW) // ★ 기본값 NEW
                .build();
    }

    public void markSent()   { this.status = OutboxStatus.SENT; }
    public void markFailed() { this.status = OutboxStatus.FAILED; }
}
