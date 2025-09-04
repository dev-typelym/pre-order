package com.app.preorder.productservice.config;

import com.app.preorder.common.messaging.command.*;
import com.app.preorder.common.messaging.event.StockEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;                // ★ 추가
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

    // ★ 환경변수/YA​ML로 조절 가능하게
    @Value("${kafka.consumer.command.concurrency:6}")
    private int commandConcurrency; // Reserve/Commit/Unreserve/Restore 용

    @Value("${kafka.consumer.event.concurrency:6}")
    private int eventConcurrency;   // StockEvent(브로드캐스트) 용

    // 공통 에러 핸들러
    private DefaultErrorHandler buildErrorHandler(KafkaTemplate<Object, Object> dltTemplate) {
        var backoff = new ExponentialBackOffWithMaxRetries(4); // 5s→10s→20s→40s
        backoff.setInitialInterval(5_000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(60_000L);

        var recoverer = new DeadLetterPublishingRecoverer(dltTemplate); // topic.DLT로 전송
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
        // 필요 시: props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);
        return props;
    }

    // ---------- Restore ----------
    @Bean
    public ConsumerFactory<String, StockRestoreRequest> stockRestoreConsumerFactory() {
        var value = new JsonDeserializer<>(StockRestoreRequest.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockRestoreRequest>
    stockRestoreKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockRestoreRequest>();
        f.setConsumerFactory(stockRestoreConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.setConcurrency(commandConcurrency); // ★ 추가
        return f;
    }

    // ---------- Reserve ----------
    @Bean
    public ConsumerFactory<String, ReserveStocksRequest> stockReserveConsumerFactory() {
        var value = new JsonDeserializer<>(ReserveStocksRequest.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReserveStocksRequest>
    stockReserveKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, ReserveStocksRequest>();
        f.setConsumerFactory(stockReserveConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.setConcurrency(commandConcurrency); // ★ 추가
        return f;
    }

    // ---------- Commit ----------
    @Bean
    public ConsumerFactory<String, CommitStocksRequest> stockCommitConsumerFactory() {
        var value = new JsonDeserializer<>(CommitStocksRequest.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CommitStocksRequest>
    stockCommitKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, CommitStocksRequest>();
        f.setConsumerFactory(stockCommitConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.setConcurrency(commandConcurrency); // ★ 추가
        return f;
    }

    // ---------- Unreserve ----------
    @Bean
    public ConsumerFactory<String, UnreserveStocksRequest> stockUnreserveConsumerFactory() {
        var value = new JsonDeserializer<>(UnreserveStocksRequest.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UnreserveStocksRequest>
    stockUnreserveKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, UnreserveStocksRequest>();
        f.setConsumerFactory(stockUnreserveConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.setConcurrency(commandConcurrency); // ★ 추가
        return f;
    }

    // ---------- StockEvent (읽기 모델/브로드캐스트) ----------
    @Bean
    public ConsumerFactory<String, StockEvent> stockEventsConsumerFactory() {
        var value = new JsonDeserializer<>(StockEvent.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StockEvent>
    stockEventsKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        var f = new ConcurrentKafkaListenerContainerFactory<String, StockEvent>();
        f.setConsumerFactory(stockEventsConsumerFactory());
        f.setCommonErrorHandler(buildErrorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.setConcurrency(eventConcurrency); // ★ 추가
        return f;
    }
}
