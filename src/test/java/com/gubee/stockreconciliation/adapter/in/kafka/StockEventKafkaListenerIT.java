package com.gubee.stockreconciliation.adapter.in.kafka;

import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
class StockEventKafkaListenerIT {

    private static final String TOPIC = "stock-events-it-" + UUID.randomUUID();
    private static final String GROUP_ID = "gubee-stock-reconciliation-it-" + UUID.randomUUID();
    private static final StockKey STOCK_KEY = new StockKey("account-kafka-001", "KAFKA-123");

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> GROUP_ID);
        registry.add("stock-reconciliation.kafka.stock-events-topic", () -> TOPIC);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private GetCurrentStockUseCase getCurrentStockUseCase;

    @Autowired
    private GetProcessedStockEventUseCase getProcessedStockEventUseCase;

    @Value("${stock-reconciliation.kafka.stock-events-topic}")
    private String topic;

    @Test
    void consumesStockEventFromKafkaAndPersistsReconciliationInMysql() {
        var payload = """
                {
                  "eventId": "evt-kafka-001",
                  "type": "STOCK_ADJUSTED",
                  "occurredAt": "2026-05-28T10:00:00Z",
                  "accountId": "account-kafka-001",
                  "sku": "KAFKA-123",
                  "available": 17,
                  "reason": "integration_test"
                }
                """;

        kafkaTemplate.send(topic, STOCK_KEY.accountId() + ":" + STOCK_KEY.sku(), payload);
        kafkaTemplate.flush();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(getCurrentStockUseCase.getCurrentStock(STOCK_KEY))
                    .hasValueSatisfying(balance -> assertThat(balance.available()).isEqualTo(17));
            assertThat(getProcessedStockEventUseCase.getProcessedEvent("evt-kafka-001"))
                    .hasValueSatisfying(processed -> assertThat(processed.status()).isEqualTo(ProcessingStatus.APPLIED));
        });
    }
}
