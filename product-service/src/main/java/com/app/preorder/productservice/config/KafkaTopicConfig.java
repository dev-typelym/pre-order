package com.app.preorder.productservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // 메인 토픽들
    @Bean
    public NewTopic stockEventsTopic() {
        return TopicBuilder.name("inventory.stock-events.v1")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic stockRestoreRequestTopic() {  // ← 이게 없으면 추가 필요
        return TopicBuilder.name("inventory.stock-restore.request.v1")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic stockReserveRequest() {
        return TopicBuilder.name("inventory.stock-reserve.request.v1")
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic stockCommitRequest() {
        return TopicBuilder.name("inventory.stock-commit.request.v1")
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic stockUnreserveRequest() {
        return TopicBuilder.name("inventory.stock-unreserve.request.v1")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic stockCommandResults() {
        return TopicBuilder.name("inventory.stock-command-results.v1")
                .partitions(6).replicas(1).build();
    }

    // DLT 토픽들(Dead Letter)
    @Bean
    public NewTopic stockEventsDlt() {
        return TopicBuilder.name("inventory.stock-events.v1.DLT")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic stockRestoreRequestDlt() {
        return TopicBuilder.name("inventory.stock-restore.request.v1.DLT")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic stockReserveRequestDlt() {
        return TopicBuilder.name("inventory.stock-reserve.request.v1.DLT")
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic stockCommitRequestDlt() {
        return TopicBuilder.name("inventory.stock-commit.request.v1.DLT")
                .partitions(6).replicas(1).build();
    }
    @Bean
    public NewTopic stockUnreserveRequestDlt() {
        return TopicBuilder.name("inventory.stock-unreserve.request.v1.DLT")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic stockCommandResultsDlt() {
        return TopicBuilder.name("inventory.stock-command-results.v1.DLT")
                .partitions(6).replicas(1).build();
    }
}
