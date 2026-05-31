package com.gubee.stockreconciliation.adapter.in.kafka.dto;

import com.gubee.stockreconciliation.domain.event.StockEventType;

import java.time.Instant;

public record StockEventMessage(
        String eventId,
        StockEventType type,
        Instant occurredAt,
        String marketplace,
        String accountId,
        String externalOrderId,
        String sku,
        Integer quantity,
        Integer available,
        Integer quantitySent,
        String reason
) {
}
