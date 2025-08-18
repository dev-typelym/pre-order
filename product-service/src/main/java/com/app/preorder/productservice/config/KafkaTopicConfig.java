package com.app.preorder.productservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic stockEventsTopic() {
        return TopicBuilder
                .name("inventory.stock-events.v1")
                .partitions(6)   // 로컬이면 3~6 아무거나
                .replicas(1)
                .build();
    }
}
