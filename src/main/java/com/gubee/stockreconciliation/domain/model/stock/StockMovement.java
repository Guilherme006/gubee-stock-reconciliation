package com.gubee.stockreconciliation.domain.model.stock;

import java.time.Instant;
import java.util.Objects;

public record StockMovement(
        String eventId,
        StockKey stockKey,
        MovementType movementType,
        int quantityDelta,
        int previousAvailable,
        int currentAvailable,
        String reason,
        Instant occurredAt
) {

    public StockMovement {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId must not be blank");
        }
        stockKey = Objects.requireNonNull(stockKey, "stockKey must not be null");
        movementType = Objects.requireNonNull(movementType, "movementType must not be null");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
