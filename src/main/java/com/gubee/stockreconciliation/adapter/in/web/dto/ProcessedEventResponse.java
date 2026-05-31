package com.gubee.stockreconciliation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento ja processado e seu status de reconciliacao.")
public record ProcessedEventResponse(
        @Schema(example = "evt-001")
        String eventId,
        @Schema(example = "STOCK_ADJUSTED")
        String type,
        @Schema(example = "account-001")
        String accountId,
        @Schema(example = "ABC-123")
        String sku,
        @Schema(example = "APPLIED")
        String status,
        @Schema(example = "Stock adjusted")
        String detail
) {
}
