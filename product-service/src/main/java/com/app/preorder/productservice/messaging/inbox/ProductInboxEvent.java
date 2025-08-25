package com.app.preorder.productservice.messaging.inbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "product_inbox_event",
        uniqueConstraints = @UniqueConstraint(name = "uq_product_inbox_msg", columnNames = "messageKey")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductInboxEvent {

    public enum InboxStatus { PENDING, PROCESSED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String messageKey; // 멱등키(예: eventId)

    @Column(nullable = false, length = 200)
    private String topic;      // 수신 토픽명(디버깅용)

    @Lob
    @Column(nullable = false)
    private String payloadJson; // 원본 페이로드(JSON)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 한 줄 주석: 새 인박스 이벤트 팩토리
    public static ProductInboxEvent of(String messageKey, String topic, String payloadJson) {
        return ProductInboxEvent.builder()
                .messageKey(messageKey)
                .topic(topic)
                .payloadJson(payloadJson)
                .status(InboxStatus.PENDING)
                .retryCount(0)
                .build();
    }

    // 한 줄 주석: 처리 완료 마킹
    public void markProcessed() { this.status = InboxStatus.PROCESSED; }

    // 한 줄 주석: 실패(영구 실패로 전환)
    public void markFailed() { this.status = InboxStatus.FAILED; }

    // 한 줄 주석: 재시도 카운트 증가
    public void incRetry() { this.retryCount += 1; }
}