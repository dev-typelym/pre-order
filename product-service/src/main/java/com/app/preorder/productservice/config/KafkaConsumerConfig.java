package com.app.preorder.productservice.config;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.event.StockEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        // spring.kafka.bootstrap-servers가 없으면 env → kafka:9092 로 폴백
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // value deserializer는 아래에서 타입별로 지정
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 컨테이너가 커밋 관리
        return props;
    }

    // ---------- StockRestoreRequest ----------
    @Bean
    public ConsumerFactory<String, StockRestoreRequest> stockRestoreConsumerFactory() {
        JsonDeserializer<StockRestoreRequest> value = new JsonDeserializer<>(StockRestoreRequest.class, false);
        value.addTrustedPackages("*");
        value.setUseTypeHeaders(false); // 타입 헤더 없이 고정 타입 역직렬화
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockRestoreRequest>
    stockRestoreKafkaListenerContainerFactory() {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockRestoreRequest>();
        f.setConsumerFactory(stockRestoreConsumerFactory());
        // 필요 시 f.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        // 필요 시 f.setConcurrency(6);  // @KafkaListener의 concurrency로도 설정 가능
        return f;
    }

    // ---------- StockEvent ----------
    @Bean
    public ConsumerFactory<String, StockEvent> stockEventsConsumerFactory() {
        JsonDeserializer<StockEvent> value = new JsonDeserializer<>(StockEvent.class, false);
        value.addTrustedPackages("*");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockEvent>
    stockEventsKafkaListenerContainerFactory() {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockEvent>();
        f.setConsumerFactory(stockEventsConsumerFactory());
        return f;
    }
}
