package com.gubee.stockreconciliation.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gubee.stockreconciliation.adapter.out.observability.StockEventMetrics;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;

class StockEventKafkaListenerTest {

    private final ProcessStockEventUseCase processStockEventUseCase = mock(ProcessStockEventUseCase.class);
    private final StockEventMetrics stockEventMetrics = mock(StockEventMetrics.class);
    private final StockEventKafkaListener listener = new StockEventKafkaListener(
            new ObjectMapper(),
            new KafkaStockEventMapper(),
            processStockEventUseCase,
            stockEventMetrics
    );

    @Test
    void acknowledgesInvalidPayloadWithoutCallingUseCase() {
        var acknowledgment = mock(Acknowledgment.class);
        var record = new ConsumerRecord<>("stock-events", 0, 1, "account-001:ABC-123", "{");

        listener.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(processStockEventUseCase, never()).process(org.mockito.ArgumentMatchers.any());
        verify(stockEventMetrics).recordRejected(org.mockito.ArgumentMatchers.eq("kafka"), anyString());
    }
}
