package com.gubee.stockreconciliation.application.service;

import com.gubee.stockreconciliation.domain.model.processing.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.processing.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.stock.StockBalance;
import com.gubee.stockreconciliation.domain.model.stock.StockEvent;
import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.model.stock.StockMovement;
import com.gubee.stockreconciliation.domain.model.stock.StockReconciliationResult;
import com.gubee.stockreconciliation.domain.policy.StockReconciler;
import com.gubee.stockreconciliation.domain.port.out.StockLedgerPort;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationLockPort;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationStatePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class StockReconciliationApplicationServiceTest {

    private static final Instant BASE_TIME = Instant.parse("2026-05-28T10:00:00Z");
    private static final StockKey STOCK_KEY = new StockKey("account-001", "ABC-123");

    private final InMemoryStockLedger ledger = new InMemoryStockLedger();
    private final InMemoryReconciliationState state = new InMemoryReconciliationState();
    private final CountingLock lock = new CountingLock();
    private final StockReconciliationApplicationService service = new StockReconciliationApplicationService(
            ledger,
            state,
            lock,
            new StockReconciler()
    );

    @Test
    void processesEventThroughLedgerAndProjectionPorts() {
        var event = StockEvent.stockAdjusted(
                "evt-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                1
        );

        var result = service.process(event);

        assertThat(result.processedEvent().status()).isEqualTo(ProcessingStatus.APPLIED);
        assertThat(result.currentAvailable()).isEqualTo(10);
        assertThat(ledger.findEventById("evt-001")).contains(event);
        assertThat(service.getCurrentStock(STOCK_KEY)).contains(new StockBalance(STOCK_KEY, 10));
        assertThat(service.getHistory(STOCK_KEY)).hasSize(1);
        assertThat(service.getProcessedEvent("evt-001"))
                .map(ProcessedStockEvent::status)
                .contains(ProcessingStatus.APPLIED);
        assertThat(lock.lockedKeys).containsExactly(STOCK_KEY);
    }

    @Test
    void recalculatesProjectionForAggregateWhenNewEventArrives() {
        service.process(StockEvent.stockAdjusted(
                "evt-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                1
        ));

        var result = service.process(StockEvent.orderCreated(
                "evt-002",
                BASE_TIME.plusSeconds(1),
                "MERCADO_LIVRE",
                "account-001",
                "ML-123456",
                "ABC-123",
                2,
                2
        ));

        assertThat(result.currentAvailable()).isEqualTo(8);
        assertThat(service.getCurrentStock(STOCK_KEY)).contains(new StockBalance(STOCK_KEY, 8));
        assertThat(service.getHistory(STOCK_KEY)).hasSize(2);
    }

    @Test
    void duplicateEventIsReturnedWithoutAppendingOrRecalculating() {
        var original = StockEvent.stockAdjusted(
                "evt-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                1
        );
        service.process(original);

        var result = service.process(StockEvent.stockAdjusted(
                "evt-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                99
        ));

        assertThat(result.processedEvent().status()).isEqualTo(ProcessingStatus.IGNORED_DUPLICATE_EVENT);
        assertThat(result.currentAvailable()).isEqualTo(10);
        assertThat(ledger.eventsById).hasSize(1);
        assertThat(state.saveCount).isEqualTo(1);
    }

    @Test
    void sameEventIdWithDifferentPayloadIsRejectedBeforeAppend() {
        service.process(StockEvent.stockAdjusted(
                "evt-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                1
        ));

        var result = service.process(StockEvent.stockAdjusted(
                "evt-001",
                BASE_TIME,
                "account-001",
                "ABC-123",
                7,
                "manual_adjustment",
                2
        ));

        assertThat(result.processedEvent().status()).isEqualTo(ProcessingStatus.REJECTED_DUPLICATE_EVENT_ID);
        assertThat(result.currentAvailable()).isEqualTo(10);
        assertThat(ledger.eventsById).hasSize(1);
        assertThat(state.saveCount).isEqualTo(1);
    }

    private static final class InMemoryStockLedger implements StockLedgerPort {

        private final Map<String, StockEvent> eventsById = new HashMap<>();
        private final Map<StockKey, List<StockEvent>> eventsByStockKey = new HashMap<>();

        @Override
        public Optional<StockEvent> findEventById(String eventId) {
            return Optional.ofNullable(eventsById.get(eventId));
        }

        @Override
        public void append(StockEvent event) {
            eventsById.put(event.eventId(), event);
            eventsByStockKey.computeIfAbsent(event.stockKey(), ignored -> new ArrayList<>()).add(event);
        }

        @Override
        public List<StockEvent> findEventsByStockKey(StockKey stockKey) {
            return List.copyOf(eventsByStockKey.getOrDefault(stockKey, List.of()));
        }
    }

    private static final class InMemoryReconciliationState implements StockReconciliationStatePort {

        private final Map<StockKey, StockBalance> balances = new HashMap<>();
        private final Map<StockKey, List<StockMovement>> histories = new HashMap<>();
        private final Map<String, ProcessedStockEvent> processedEvents = new HashMap<>();
        private int saveCount;

        @Override
        public void saveReconciliation(StockKey stockKey, StockReconciliationResult result) {
            balances.put(stockKey, new StockBalance(stockKey, result.availableFor(stockKey)));
            histories.put(stockKey, result.movements());
            result.processedEvents().forEach(processed -> processedEvents.put(processed.event().eventId(), processed));
            saveCount++;
        }

        @Override
        public Optional<StockBalance> findCurrentStock(StockKey stockKey) {
            return Optional.ofNullable(balances.get(stockKey));
        }

        @Override
        public List<StockMovement> findHistory(StockKey stockKey) {
            return List.copyOf(histories.getOrDefault(stockKey, List.of()));
        }

        @Override
        public Optional<ProcessedStockEvent> findProcessedEvent(String eventId) {
            return Optional.ofNullable(processedEvents.get(eventId));
        }
    }

    private static final class CountingLock implements StockReconciliationLockPort {

        private final List<StockKey> lockedKeys = new ArrayList<>();

        @Override
        public <T> T withStockLock(StockKey stockKey, Supplier<T> operation) {
            lockedKeys.add(stockKey);
            return operation.get();
        }
    }
}
