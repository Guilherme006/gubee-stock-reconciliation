package com.gubee.stockreconciliation.adapter.in.web.dto;

import com.gubee.stockreconciliation.domain.event.StockEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "Evento de estoque recebido por REST ou publicado no Kafka.")
public record StockEventRequest(
        @Schema(example = "evt-001")
        @NotBlank String eventId,
        @Schema(example = "STOCK_ADJUSTED")
        @NotNull StockEventType type,
        @Schema(example = "2026-05-28T10:00:00Z")
        @NotNull Instant occurredAt,
        @Schema(example = "MERCADO_LIVRE")
        String marketplace,
        @Schema(example = "account-001")
        @NotBlank String accountId,
        @Schema(example = "ML-123456")
        String externalOrderId,
        @Schema(example = "ABC-123")
        @NotBlank String sku,
        @Schema(example = "2", description = "Quantidade do pedido para eventos de pedido.")
        Integer quantity,
        @Schema(example = "10", description = "Saldo absoluto para STOCK_ADJUSTED.")
        Integer available,
        @Schema(example = "8", description = "Quantidade enviada ao marketplace para STOCK_SYNC_SENT.")
        Integer quantitySent,
        @Schema(example = "manual_adjustment")
        String reason
) {
}
