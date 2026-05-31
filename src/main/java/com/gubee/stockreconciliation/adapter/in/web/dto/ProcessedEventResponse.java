package com.gubee.stockreconciliation.adapter.in.web.dto;

public record ProcessedEventResponse(
        String eventId,
        String type,
        String accountId,
        String sku,
        String status,
        String detail
) {
}
