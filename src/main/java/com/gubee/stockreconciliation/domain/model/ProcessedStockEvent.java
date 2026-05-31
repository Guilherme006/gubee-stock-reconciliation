package com.gubee.stockreconciliation.domain.model;

import java.util.Objects;

public record ProcessedStockEvent(
        StockEvent event,
        ProcessingStatus status,
        String detail
) {

    public ProcessedStockEvent {
        event = Objects.requireNonNull(event, "event must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
    }
}
