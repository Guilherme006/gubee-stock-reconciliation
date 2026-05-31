package com.gubee.stockreconciliation.application.service;

import com.gubee.stockreconciliation.domain.model.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.ProcessStockEventResult;
import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.StockBalance;
import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.model.StockMovement;
import com.gubee.stockreconciliation.domain.policy.StockReconciler;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetStockHistoryUseCase;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.out.StockLedgerPort;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationLockPort;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationStatePort;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StockReconciliationApplicationService implements
        ProcessStockEventUseCase,
        GetCurrentStockUseCase,
        GetStockHistoryUseCase,
        GetProcessedStockEventUseCase {

    private final StockLedgerPort stockLedgerPort;
    private final StockReconciliationStatePort stockReconciliationStatePort;
    private final StockReconciliationLockPort stockReconciliationLockPort;
    private final StockReconciler stockReconciler;

    public StockReconciliationApplicationService(
            StockLedgerPort stockLedgerPort,
            StockReconciliationStatePort stockReconciliationStatePort,
            StockReconciliationLockPort stockReconciliationLockPort,
            StockReconciler stockReconciler
    ) {
        this.stockLedgerPort = Objects.requireNonNull(stockLedgerPort, "stockLedgerPort must not be null");
        this.stockReconciliationStatePort = Objects.requireNonNull(
                stockReconciliationStatePort,
                "stockReconciliationStatePort must not be null"
        );
        this.stockReconciliationLockPort = Objects.requireNonNull(
                stockReconciliationLockPort,
                "stockReconciliationLockPort must not be null"
        );
        this.stockReconciler = Objects.requireNonNull(stockReconciler, "stockReconciler must not be null");
    }

    @Override
    public ProcessStockEventResult process(StockEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        return stockReconciliationLockPort.withStockLock(event.stockKey(), () -> processWithLock(event));
    }

    @Override
    public Optional<StockBalance> getCurrentStock(StockKey stockKey) {
        Objects.requireNonNull(stockKey, "stockKey must not be null");
        return stockReconciliationStatePort.findCurrentStock(stockKey);
    }

    @Override
    public List<StockMovement> getHistory(StockKey stockKey) {
        Objects.requireNonNull(stockKey, "stockKey must not be null");
        return stockReconciliationStatePort.findHistory(stockKey);
    }

    @Override
    public Optional<ProcessedStockEvent> getProcessedEvent(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        return stockReconciliationStatePort.findProcessedEvent(eventId);
    }

    private ProcessStockEventResult processWithLock(StockEvent event) {
        var existingEvent = stockLedgerPort.findEventById(event.eventId());
        if (existingEvent.isPresent()) {
            return duplicateResult(event, existingEvent.get());
        }

        stockLedgerPort.append(event);

        var events = stockLedgerPort.findEventsByStockKey(event.stockKey());
        var reconciliationResult = stockReconciler.reconcile(events);
        stockReconciliationStatePort.saveReconciliation(event.stockKey(), reconciliationResult);

        var processedEvent = reconciliationResult.processedEvents().stream()
                .filter(processed -> processed.event().eventId().equals(event.eventId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Processed event was not produced by reconciliation"));

        return new ProcessStockEventResult(
                processedEvent,
                reconciliationResult.availableFor(event.stockKey()),
                reconciliationResult.movements()
        );
    }

    private ProcessStockEventResult duplicateResult(StockEvent event, StockEvent existingEvent) {
        var status = event.samePayloadAs(existingEvent)
                ? ProcessingStatus.IGNORED_DUPLICATE_EVENT
                : ProcessingStatus.REJECTED_DUPLICATE_EVENT_ID;
        var detail = event.samePayloadAs(existingEvent)
                ? "Event id was already processed with the same payload"
                : "Event id was already processed with a different payload";
        var processedEvent = new ProcessedStockEvent(event, status, detail);
        var currentAvailable = stockReconciliationStatePort.findCurrentStock(event.stockKey())
                .map(StockBalance::available)
                .orElse(0);

        return new ProcessStockEventResult(
                processedEvent,
                currentAvailable,
                stockReconciliationStatePort.findHistory(event.stockKey())
        );
    }
}
