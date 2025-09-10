// member-service/src/main/java/com/app/preorder/memberservice/config/KafkaPublisherConfig.java
package com.app.preorder.memberservice.config;

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

    @Bean @Primary
    public ProducerFactory<Object, Object> genericProducerFactory(
            @Value("${spring.kafka.bootstrap-servers:kafka:9092}") String bootstrap) {
        return new DefaultKafkaProducerFactory<>(baseProps(bootstrap));
    }

    @Bean @Primary
    public KafkaTemplate<Object, Object> kafkaTemplate(
            ProducerFactory<Object, Object> pf) {
        return new KafkaTemplate<>(pf);
    }
}
