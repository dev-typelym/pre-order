// order-service/src/main/java/com/app/preorder/orderservice/config/KafkaPublisherConfig.java
package com.app.preorder.orderservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaPublisherConfig {

    /** 공통 Producer 설정 */
    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 신뢰성
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 10);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);

        // 컨슈머에서 type headers 끈 구성과 대칭
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return props;
    }

    /** 여러 DTO 타입을 공통으로 전송하는 제네릭 템플릿 (OutboxProcessor/DLT에서 사용) */
    @Bean
    public ProducerFactory<Object, Object> genericProducerFactory() {
        return new DefaultKafkaProducerFactory<>(baseProps());
    }

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }
}
