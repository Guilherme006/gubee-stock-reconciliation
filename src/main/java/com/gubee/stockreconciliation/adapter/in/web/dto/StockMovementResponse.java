package com.gubee.stockreconciliation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Movimento gerado pelo ledger de reconciliacao.")
public record StockMovementResponse(
        @Schema(example = "evt-001")
        String eventId,
        @Schema(example = "ORDER_RESERVED")
        String movementType,
        @Schema(example = "-2")
        int quantityDelta,
        @Schema(example = "10")
        int previousAvailable,
        @Schema(example = "8")
        int currentAvailable,
        @Schema(example = "order_created")
        String reason,
        @Schema(example = "2026-05-28T10:00:00Z")
        Instant occurredAt
) {
}
