package com.app.preorder.productservice.config;

import com.app.preorder.common.messaging.topics.KafkaTopics; // ✅ 추가
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // ===== 공통 파라미터 =====
    private static final int PARTITIONS = 6;
    private static final int REPLICAS = 1; // 운영은 보통 3 권장

    // 보존 기간(ms)
    private static final long RETAIN_REQ_MS     = 259_200_000L;   // 3일
    private static final long RETAIN_RESULT_MS  = 604_800_000L;   // 7일
    private static final long RETAIN_EVENT_MS   = 2_592_000_000L; // 30일
    private static final long RETAIN_DLT_MS     = 1_209_600_000L; // 14일

    private NewTopic topic(String name, long retentionMs) {
        return TopicBuilder.name(name)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config("cleanup.policy", "delete")
                .config("retention.ms", Long.toString(retentionMs))
                .build();
    }

    // ===== 메인 토픽들 =====
    @Bean
    public NewTopic stockEventsTopic() {
        // 재고 변화 이벤트: 분석/모니터링을 위해 길게 유지(30일)
        return topic("inventory.stock-events.v1", RETAIN_EVENT_MS);
    }

    @Bean
    public NewTopic stockRestoreRequestTopic() {
        // 요청 계열: 3일
        return topic("inventory.stock-restore.request.v1", RETAIN_REQ_MS);
    }

    @Bean
    public NewTopic stockReserveRequest() {
        return topic("inventory.stock-reserve.request.v1", RETAIN_REQ_MS);
    }

    @Bean
    public NewTopic stockCommitRequest() {
        return topic("inventory.stock-commit.request.v1", RETAIN_REQ_MS);
    }

    @Bean
    public NewTopic stockUnreserveRequest() {
        return topic("inventory.stock-unreserve.request.v1", RETAIN_REQ_MS);
    }

    @Bean
    public NewTopic stockCommandResults() {
        // 결과 계열: 7일
        return topic("inventory.stock-command-results.v1", RETAIN_RESULT_MS);
    }

    // ✅ 추가: 상품 상태 변경 이벤트 (30일 보관)
    @Bean
    public NewTopic productStatusChangedV1() {
        return topic(KafkaTopics.PRODUCT_STATUS_CHANGED_V1, RETAIN_EVENT_MS);
    }

    // ===== DLT 토픽들 (Dead Letter) =====
    @Bean
    public NewTopic stockEventsDlt() {
        // 장애 분석/수동 재처리를 위해 14일
        return topic("inventory.stock-events.v1.DLT", RETAIN_DLT_MS);
    }

    @Bean
    public NewTopic stockRestoreRequestDlt() {
        return topic("inventory.stock-restore.request.v1.DLT", RETAIN_DLT_MS);
    }

    @Bean
    public NewTopic stockReserveRequestDlt() {
        return topic("inventory.stock-reserve.request.v1.DLT", RETAIN_DLT_MS);
    }

    @Bean
    public NewTopic stockCommitRequestDlt() {
        return topic("inventory.stock-commit.request.v1.DLT", RETAIN_DLT_MS);
    }

    @Bean
    public NewTopic stockUnreserveRequestDlt() {
        return topic("inventory.stock-unreserve.request.v1.DLT", RETAIN_DLT_MS);
    }

    @Bean
    public NewTopic stockCommandResultsDlt() {
        return topic("inventory.stock-command-results.v1.DLT", RETAIN_DLT_MS);
    }

    @Bean
    public NewTopic productStatusChangedV1Dlt() {
        return topic(KafkaTopics.PRODUCT_STATUS_CHANGED_V1 + ".DLT", RETAIN_DLT_MS);
    }
}
