package com.gubee.stockreconciliation.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

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
}
