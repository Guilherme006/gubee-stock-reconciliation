package com.gubee.stockreconciliation.domain.model;

import java.util.List;
import java.util.Objects;

public record ProcessStockEventResult(
        ProcessedStockEvent processedEvent,
        int currentAvailable,
        List<StockMovement> movements
) {

    public ProcessStockEventResult {
        processedEvent = Objects.requireNonNull(processedEvent, "processedEvent must not be null");
        if (currentAvailable < 0) {
            throw new IllegalArgumentException("currentAvailable must be non-negative");
        }
        movements = List.copyOf(movements);
    }
}
