// order-service/src/main/java/com/app/preorder/orderservice/config/KafkaConsumerConfig.java
package com.app.preorder.orderservice.config;

import com.app.preorder.common.messaging.event.StockCommandResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
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

    @Value("${kafka.consumer.stock-result.concurrency:4}")
    private int stockResultConcurrency; // 성능 튜닝을 프로퍼티로

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 필요 시: props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);
        return props;
    }

    private DefaultErrorHandler buildErrorHandler(KafkaTemplate<Object, Object> dltTemplate) {
        var backoff = new ExponentialBackOffWithMaxRetries(4); // 5s → 10s → 20s → 40s
        backoff.setInitialInterval(5_000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(60_000L);

        var recoverer = new DeadLetterPublishingRecoverer(dltTemplate); // topic.DLT 로 보냄
        var handler = new DefaultErrorHandler(recoverer, backoff);

        // 멱등/유니크 충돌 등은 재시도 불필요
        handler.addNotRetryableExceptions(DataIntegrityViolationException.class);
        return handler;
    }

    @Bean
    public ConsumerFactory<String, StockCommandResult> stockCommandResultConsumerFactory() {
        var value = new JsonDeserializer<>(StockCommandResult.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging"); // 반드시 좁혀주세요
        value.setUseTypeHeaders(false); // 타입 헤더 OFF 전략과 대칭
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockCommandResult>
    stockCommandResultKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockCommandResult>();
        f.setConsumerFactory(stockCommandResultConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD); // 레코드 단위 커밋
        f.setConcurrency(stockResultConcurrency); // 병렬 처리(파티션 수와 맞추면 베스트)
        return f;
    }
}
