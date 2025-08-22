package com.app.preorder.orderservice.config;

import com.app.preorder.common.messaging.topics.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic stockRestoreRequestTopic() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1)
                .partitions(8)
                .replicas(1)
                .build();
    }
}
