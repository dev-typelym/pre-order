// product-service/src/main/java/com/app/preorder/productservice/config/KafkaPublisherConfig.java
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

    /** 프로듀서 공통 옵션 */
    private Map<String, Object> baseProps(String bootstrap) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 10);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return props;
    }

    /** 여러 타입(payload)을 보낼 수 있는 제네릭 ProducerFactory/KafkaTemplate */
    @Bean
    @Primary
    public ProducerFactory<Object, Object> genericProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:kafka:9092}") String bootstrap
    ) {
        return new DefaultKafkaProducerFactory<>(baseProps(bootstrap));
    }

    /** DeadLetterPublishingRecoverer + OutboxProcessor가 주입받아 쓰는 템플릿 */
    @Bean
    @Primary
    public KafkaTemplate<Object, Object> kafkaTemplate(
            ProducerFactory<Object, Object> pf
    ) {
        return new KafkaTemplate<>(pf);
    }
}
