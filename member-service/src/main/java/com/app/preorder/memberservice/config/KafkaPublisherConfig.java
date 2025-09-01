// member-service/src/main/java/com/app/preorder/memberservice/config/KafkaPublisherConfig.java
package com.app.preorder.memberservice.config;

import com.app.preorder.common.messaging.event.MemberRegisterEvent;
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
    public ProducerFactory<String, MemberRegisterEvent> memberRegisterProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"),
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                        JsonSerializer.ADD_TYPE_INFO_HEADERS, false,           // 컨슈머 setUseTypeHeaders(false)와 대칭
                        ProducerConfig.ACKS_CONFIG, "all",                     // 신뢰성 4줄
                        ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                        ProducerConfig.RETRIES_CONFIG, 10,
                        ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000
                )
        );
    }

    @Bean
    public KafkaTemplate<String, MemberRegisterEvent> memberRegisterKafkaTemplate(
            ProducerFactory<String, MemberRegisterEvent> pf) {
        return new KafkaTemplate<>(pf);
    }
}
