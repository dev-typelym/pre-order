package com.app.preorder.productservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaPublisherConfig {

    private Map<String, Object> baseProps(String bootstrap) {
        Map<String, Object> props = new HashMap<>();
        // 기본
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // 타입 헤더 OFF (컨슈머 전략과 매칭)

        // 신뢰성
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 10);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);

        // 🔹 권장 보강(성능/안전)
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // idem과 조합 안전
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);                      // 소량 버퍼링
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 65_536);                 // ~64KiB
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");            // 전송량↓ 처리량↑
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15_000);         // 타임아웃 명시

        return props;
    }

    @Bean
    @Primary
    public ProducerFactory<Object, Object> genericProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:kafka:9092}") String bootstrap
    ) {
        return new DefaultKafkaProducerFactory<>(baseProps(bootstrap));
    }

    @Bean
    @Primary
    public KafkaTemplate<Object, Object> kafkaTemplate(ProducerFactory<Object, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}
