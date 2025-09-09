package com.app.preorder.orderservice.config;

import com.app.preorder.common.messaging.topics.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 6;
    private static final int REPLICAS = 1; // 운영은 통상 3 권장
    private static final long RETAIN_RESULT_MS = 604_800_000L;   // 7d
    private static final long RETAIN_DLT_MS    = 1_209_600_000L; // 14d

    private NewTopic topic(String name, long retentionMs) {
        return TopicBuilder.name(name)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config("cleanup.policy", "delete")
                .config("retention.ms", Long.toString(retentionMs))
                .build();
    }

    @Bean
    public NewTopic orderCompletedV1() {
        return topic(KafkaTopics.ORDER_COMPLETED_V1, RETAIN_RESULT_MS);
    }

    @Bean
    public NewTopic orderCompletedV1Dlt() {
        return topic(KafkaTopics.ORDER_COMPLETED_V1 + ".DLT", RETAIN_DLT_MS);
    }
}
