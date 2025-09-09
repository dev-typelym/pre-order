package com.app.preorder.cartservice.config;

import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.event.MemberDeactivatedEvent; // ✅ 추가
import com.app.preorder.common.messaging.event.OrderCompletedEvent;   // ✅ 추가
import com.app.preorder.common.messaging.event.StockEvent;            // ✅ 추가
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${kafka.consumer.cart-create.concurrency:3}")
    private int cartConcurrency;

    @Value("${kafka.consumer.cart-inbox.concurrency:3}") // ✅ 추가: 인박스 공통 컨커런시
    private int inboxConcurrency;

    @Bean
    public Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return props;
    }

    /* ===================== CartCreate (기존) ===================== */

    @Bean
    public ConsumerFactory<String, CartCreateRequest> cartCreateConsumerFactory() {
        JsonDeserializer<CartCreateRequest> value = new JsonDeserializer<>(CartCreateRequest.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    private DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> dltTemplate) {
        ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(4);
        backoff.setInitialInterval(5_000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(60_000L);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(DataIntegrityViolationException.class);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CartCreateRequest>
    cartCreateListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, CartCreateRequest> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(cartCreateConsumerFactory());
        f.setCommonErrorHandler(errorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.getContainerProperties().setMissingTopicsFatal(false); // ✅ 구독자 안전
        f.setConcurrency(cartConcurrency);
        return f;
    }

    /* ===================== Inbox: MemberDeactivated ===================== */

    @Bean
    public ConsumerFactory<String, MemberDeactivatedEvent> memberDeactivatedConsumerFactory() {
        JsonDeserializer<MemberDeactivatedEvent> value = new JsonDeserializer<>(MemberDeactivatedEvent.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MemberDeactivatedEvent>
    memberDeactivatedKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, MemberDeactivatedEvent> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(memberDeactivatedConsumerFactory());
        f.setCommonErrorHandler(errorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.getContainerProperties().setMissingTopicsFatal(false); // ✅ 구독자 안전
        f.setConcurrency(inboxConcurrency);
        return f;
    }

    /* ===================== Inbox: StockEvent (SOLD_OUT 등) ===================== */

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
        ConcurrentKafkaListenerContainerFactory<String, StockEvent> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(stockEventsConsumerFactory());
        f.setCommonErrorHandler(errorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.getContainerProperties().setMissingTopicsFatal(false); // ✅ 구독자 안전
        f.setConcurrency(inboxConcurrency);
        return f;
    }

    /* ===================== Inbox: OrderCompleted ===================== */

    @Bean
    public ConsumerFactory<String, OrderCompletedEvent> orderCompletedConsumerFactory() {
        JsonDeserializer<OrderCompletedEvent> value = new JsonDeserializer<>(OrderCompletedEvent.class, false);
        value.addTrustedPackages("com.app.preorder.common.messaging");
        value.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(baseProps(), new StringDeserializer(), value);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent>
    orderCompletedKafkaListenerContainerFactory(KafkaTemplate<Object, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, OrderCompletedEvent> f =
                new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(orderCompletedConsumerFactory());
        f.setCommonErrorHandler(errorHandler(kafkaTemplate));
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        f.getContainerProperties().setMissingTopicsFatal(false); // ✅ 구독자 안전
        f.setConcurrency(inboxConcurrency);
        return f;
    }

    /** ===================== DLT/Outbox 용 제네릭 프로듀서 (기존) ===================== */
    @Bean
    public ProducerFactory<Object, Object> genericProducerFactory() {
        Map<String, Object> p = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("SPRING_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092"),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                JsonSerializer.ADD_TYPE_INFO_HEADERS, false,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true
        );
        return new DefaultKafkaProducerFactory<>(p);
    }

    @Bean
    public KafkaTemplate<Object, Object> kafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }
}
