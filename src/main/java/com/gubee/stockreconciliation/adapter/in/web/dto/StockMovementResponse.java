package com.gubee.stockreconciliation.adapter.in.web.dto;

import java.time.Instant;

public record StockMovementResponse(
        String eventId,
        String movementType,
        int quantityDelta,
        int previousAvailable,
        int currentAvailable,
        String reason,
        Instant occurredAt
) {
}
