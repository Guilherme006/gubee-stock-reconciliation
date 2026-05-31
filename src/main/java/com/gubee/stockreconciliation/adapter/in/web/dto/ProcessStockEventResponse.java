package com.gubee.stockreconciliation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado do processamento de um evento de estoque.")
public record ProcessStockEventResponse(
        @Schema(example = "evt-001")
        String eventId,
        @Schema(example = "APPLIED")
        String status,
        @Schema(example = "Stock adjusted")
        String detail,
        @Schema(example = "10")
        int currentAvailable,
        List<StockMovementResponse> movements
) {
}
