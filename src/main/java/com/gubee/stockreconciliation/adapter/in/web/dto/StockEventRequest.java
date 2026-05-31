package com.gubee.stockreconciliation.adapter.in.web.dto;

import com.gubee.stockreconciliation.domain.event.StockEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record StockEventRequest(
        @NotBlank String eventId,
        @NotNull StockEventType type,
        @NotNull Instant occurredAt,
        String marketplace,
        @NotBlank String accountId,
        String externalOrderId,
        @NotBlank String sku,
        Integer quantity,
        Integer available,
        Integer quantitySent,
        String reason
) {
}
