package com.gubee.stockreconciliation.adapter.out.persistence;

import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetStockHistoryUseCase;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false"
})
@ActiveProfiles("integration-test")
class StockReconciliationPersistenceIT {

    private static final Instant BASE_TIME = Instant.parse("2026-05-28T10:00:00Z");
    private static final StockKey STOCK_KEY = new StockKey("account-001", "ABC-123");

    @Autowired
    private ProcessStockEventUseCase processStockEventUseCase;

    @Autowired
    private GetCurrentStockUseCase getCurrentStockUseCase;

    @Autowired
    private GetStockHistoryUseCase getStockHistoryUseCase;

    @Autowired
    private GetProcessedStockEventUseCase getProcessedStockEventUseCase;

    @Test
    void persistsLedgerProjectionAndHistoryInMysql() {
        processStockEventUseCase.process(StockEvent.stockAdjusted(
                "evt-it-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                1
        ));
        var result = processStockEventUseCase.process(StockEvent.orderCreated(
                "evt-it-002",
                BASE_TIME.plusSeconds(1),
                "MERCADO_LIVRE",
                "account-001",
                "ML-123456",
                "ABC-123",
                2,
                2
        ));

        assertThat(result.currentAvailable()).isEqualTo(8);
        assertThat(getCurrentStockUseCase.getCurrentStock(STOCK_KEY))
                .hasValueSatisfying(balance -> assertThat(balance.available()).isEqualTo(8));
        assertThat(getStockHistoryUseCase.getHistory(STOCK_KEY)).hasSize(2);
        assertThat(getProcessedStockEventUseCase.getProcessedEvent("evt-it-002"))
                .hasValueSatisfying(processed -> assertThat(processed.status()).isEqualTo(ProcessingStatus.APPLIED));
    }

    @Test
    void processesConcurrentDuplicateEventIdOnlyOnce() throws Exception {
        var event = StockEvent.stockAdjusted(
                "evt-it-concurrent-001",
                BASE_TIME.plusSeconds(10),
                "account-concurrent-001",
                "CONCURRENT-123",
                15,
                "manual_adjustment",
                1
        );
        var stockKey = event.stockKey();
        Callable<ProcessingStatus> task = () -> processStockEventUseCase.process(event).processedEvent().status();
        var executor = Executors.newFixedThreadPool(2);

        try {
            var results = executor.invokeAll(java.util.List.of(task, task));
            var statuses = results.stream()
                    .map(result -> {
                        try {
                            return result.get(10, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertThat(statuses).contains(ProcessingStatus.APPLIED, ProcessingStatus.IGNORED_DUPLICATE_EVENT);
            assertThat(getCurrentStockUseCase.getCurrentStock(stockKey))
                    .hasValueSatisfying(balance -> assertThat(balance.available()).isEqualTo(15));
            assertThat(getStockHistoryUseCase.getHistory(stockKey)).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
