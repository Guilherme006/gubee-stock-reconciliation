package com.gubee.stockreconciliation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@Profile("!test")
@EnableConfigurationProperties(StockReconciliationKafkaProperties.class)
class KafkaConsumerConfig {

    @Bean
    NewTopic stockEventsTopic(StockReconciliationKafkaProperties properties) {
        return TopicBuilder.name(properties.stockEventsTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic stockEventsDeadLetterTopic(StockReconciliationKafkaProperties properties) {
        return TopicBuilder.name(properties.deadLetterTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler stockEventKafkaErrorHandler(
            KafkaOperations<Object, Object> kafkaOperations,
            StockReconciliationKafkaProperties properties
    ) {
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(properties.deadLetterTopic(), record.partition())
        );
        var errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(properties.retryBackoffMillis(), properties.retryMaxAttempts())
        );
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
