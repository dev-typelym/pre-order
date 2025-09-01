package com.app.preorder.memberservice.config;

import com.app.preorder.common.messaging.command.CartCreateRequest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaPublisherConfig {

    @Bean
    public ProducerFactory<String, CartCreateRequest> cartCreateProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:kafka:9092}") String bootstrapServers
    ) {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                        JsonSerializer.ADD_TYPE_INFO_HEADERS, false,   // 컨슈머와 대칭
                        ProducerConfig.ACKS_CONFIG, "all",
                        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                        ProducerConfig.RETRIES_CONFIG, 10,
                        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000
                )
        );
    }

    @Bean
    public KafkaTemplate<String, CartCreateRequest> cartCreateKafkaTemplate(
            ProducerFactory<String, CartCreateRequest> pf
    ) {
        return new KafkaTemplate<>(pf);
    }
}
