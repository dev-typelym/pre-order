package com.app.preorder.memberservice.config;

import com.app.preorder.common.messaging.topics.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic memberRegisterEventsV1() {
        return TopicBuilder.name(KafkaTopics.MEMBER_REGISTER_EVENTS_V1)
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic memberRegisterEventsV1Dlt() {
        return TopicBuilder.name(KafkaTopics.MEMBER_REGISTER_EVENTS_V1 + ".DLT")
                .partitions(3).replicas(1).build();
    }
}