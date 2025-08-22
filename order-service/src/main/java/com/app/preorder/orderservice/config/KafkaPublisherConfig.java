package com.app.preorder.orderservice.config;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
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
    public ProducerFactory<String, StockRestoreRequest> stockRestoreProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"),
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                        JsonSerializer.ADD_TYPE_INFO_HEADERS, false
                )
        );
    }

    @Bean
    public KafkaTemplate<String, StockRestoreRequest> stockRestoreKafkaTemplate(
            ProducerFactory<String, StockRestoreRequest> pf) {
        return new KafkaTemplate<>(pf);
    }
}
