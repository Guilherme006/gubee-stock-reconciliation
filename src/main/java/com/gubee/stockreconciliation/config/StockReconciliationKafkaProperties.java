package com.gubee.stockreconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-reconciliation.kafka")
public record StockReconciliationKafkaProperties(
        String stockEventsTopic,
        String deadLetterTopic,
        Integer retryMaxAttempts,
        Long retryBackoffMillis
) {

    public StockReconciliationKafkaProperties {
        if (stockEventsTopic == null || stockEventsTopic.isBlank()) {
            stockEventsTopic = "stock-events";
        }
        if (deadLetterTopic == null || deadLetterTopic.isBlank()) {
            deadLetterTopic = stockEventsTopic + "-dlt";
        }
        if (retryMaxAttempts == null || retryMaxAttempts < 0) {
            retryMaxAttempts = 3;
        }
        if (retryBackoffMillis == null || retryBackoffMillis < 0) {
            retryBackoffMillis = 1000L;
        }
    }
}
