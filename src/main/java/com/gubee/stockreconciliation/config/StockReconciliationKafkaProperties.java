package com.gubee.stockreconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stock-reconciliation.kafka")
public record StockReconciliationKafkaProperties(String stockEventsTopic) {

    public StockReconciliationKafkaProperties {
        if (stockEventsTopic == null || stockEventsTopic.isBlank()) {
            stockEventsTopic = "stock-events";
        }
    }
}
