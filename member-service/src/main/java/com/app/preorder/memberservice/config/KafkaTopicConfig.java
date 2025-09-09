package com.app.preorder.memberservice.config;

import com.app.preorder.common.messaging.topics.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    /** 멤버→카트 커맨드 토픽 */
    @Bean
    public NewTopic memberCartCreateRequestV1() {
        return TopicBuilder.name(KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic memberDeactivatedV1() {
        return TopicBuilder.name(KafkaTopics.MEMBER_DEACTIVATED_V1)
                .partitions(3)   // 멤버 기준 직렬화면 3도 충분, 필요시 6
                .replicas(1)
                .build();
    }

    /** DLT (컨슈머에서 DeadLetterPublishingRecoverer 사용 시) */
    @Bean
    public NewTopic memberCartCreateRequestV1Dlt() {
        return TopicBuilder.name(KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1 + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic memberDeactivatedV1Dlt() {
        return TopicBuilder.name(KafkaTopics.MEMBER_DEACTIVATED_V1 + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}