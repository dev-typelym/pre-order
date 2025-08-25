package com.app.preorder.productservice.config;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.event.StockEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    // 공통 backoff/handler 빌더
    private DefaultErrorHandler buildErrorHandler(KafkaTemplate<Object, Object> dltTemplate) {
        var backoff = new ExponentialBackOffWithMaxRetries(4); // 5s→10s→20s→40s
        backoff.setInitialInterval(5_000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(60_000L);

        var recoverer = new DeadLetterPublishingRecoverer(dltTemplate);
        var handler = new DefaultErrorHandler(recoverer, backoff);

        // 인박스 UNIQUE 충돌은 재시도 불필요
        handler.addNotRetryableExceptions(DataIntegrityViolationException.class);
        return handler;
    }

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // (선택) props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);
        return props;
    }

    // ---------- StockRestoreRequest ----------
    @Bean
    public ConsumerFactory<String, StockRestoreRequest> stockRestoreConsumerFactory() {
        JsonDeserializer<StockRestoreRequest> value = new JsonDeserializer<>(StockRestoreRequest.class, false);
        // 보안 측면에서 정확한 패키지로 제한 권장
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockRestoreRequest>
    stockRestoreKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockRestoreRequest>();
        f.setConsumerFactory(stockRestoreConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));     // ★ 전역 에러 핸들러
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD); // ★ 레코드 단위
        // f.setConcurrency(6); // 필요 시
        return f;
    }

    // ---------- StockEvent ----------
    @Bean
    public ConsumerFactory<String, StockEvent> stockEventsConsumerFactory() {
        JsonDeserializer<StockEvent> value = new JsonDeserializer<>(StockEvent.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockEvent>
    stockEventsKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockEvent>();
        f.setConsumerFactory(stockEventsConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));     // ★ 동일 정책
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return f;
    }
}