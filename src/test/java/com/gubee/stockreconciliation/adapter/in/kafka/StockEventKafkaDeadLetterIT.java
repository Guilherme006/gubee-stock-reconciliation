package com.gubee.stockreconciliation.adapter.in.kafka;

import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetStockHistoryUseCase;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.KafkaTestUtils;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
class StockEventKafkaDeadLetterIT {

    private static final String TOPIC = "stock-events-it-" + UUID.randomUUID();
    private static final String DLT_TOPIC = TOPIC + "-dlt";
    private static final String GROUP_ID = "gubee-stock-reconciliation-dlt-it-" + UUID.randomUUID();

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.group-id", () -> GROUP_ID);
        registry.add("stock-reconciliation.kafka.stock-events-topic", () -> TOPIC);
        registry.add("stock-reconciliation.kafka.dead-letter-topic", () -> DLT_TOPIC);
        registry.add("stock-reconciliation.kafka.retry-max-attempts", () -> 1);
        registry.add("stock-reconciliation.kafka.retry-backoff-millis", () -> 10);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private ProcessStockEventUseCase processStockEventUseCase;

    @MockBean
    private GetCurrentStockUseCase getCurrentStockUseCase;

    @MockBean
    private GetProcessedStockEventUseCase getProcessedStockEventUseCase;

    @MockBean
    private GetStockHistoryUseCase getStockHistoryUseCase;

    @Value("${stock-reconciliation.kafka.stock-events-topic}")
    private String topic;

    @Value("${stock-reconciliation.kafka.dead-letter-topic}")
    private String deadLetterTopic;

    @Test
    void publishesProcessingFailuresToDeadLetterTopic() {
        when(processStockEventUseCase.process(any(StockEvent.class)))
                .thenThrow(new IllegalStateException("database unavailable"));
        var payload = """
                {
                  "eventId": "evt-kafka-dlt-001",
                  "type": "STOCK_ADJUSTED",
                  "occurredAt": "2026-05-28T10:00:00Z",
                  "accountId": "account-kafka-dlt-001",
                  "sku": "KAFKA-DLT-123",
                  "available": 17,
                  "reason": "dead_letter_test"
                }
                """;

        kafkaTemplate.send(topic, "account-kafka-dlt-001:KAFKA-DLT-123", payload);
        kafkaTemplate.flush();

        try (var consumer = deadLetterConsumer()) {
            consumer.subscribe(java.util.List.of(deadLetterTopic));

            var record = KafkaTestUtils.getSingleRecord(consumer, deadLetterTopic, Duration.ofSeconds(20));

            assertThat(record.key()).isEqualTo("account-kafka-dlt-001:KAFKA-DLT-123");
            assertThat(record.value()).contains("\"eventId\": \"evt-kafka-dlt-001\"");
            assertThat(record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_FQCN)).isNotNull();
            assertThat(record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC)).isNotNull();
        }
    }

    private static Consumer<String, String> deadLetterConsumer() {
        var consumerProperties = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(),
                "dlt-consumer-" + UUID.randomUUID(),
                "true"
        );
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                new StringDeserializer(),
                new StringDeserializer()
        ).createConsumer();
    }
}
