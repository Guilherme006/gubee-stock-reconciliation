package com.gubee.stockreconciliation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@JsonPropertyOrder({
        "eventId",
        "movementType",
        "quantityDelta",
        "previousAvailable",
        "currentAvailable",
        "reason",
        "occurredAt"
})
@Schema(
        name = "StockMovementResponse",
        title = "Movimento do ledger",
        description = "Movimento imutavel gerado pela reconciliacao e usado para compor o historico do estoque.",
        example = """
                {
                  "eventId": "evt-001",
                  "movementType": "STOCK_ADJUSTED",
                  "quantityDelta": 10,
                  "previousAvailable": 0,
                  "currentAvailable": 10,
                  "reason": "manual_adjustment",
                  "occurredAt": "2026-05-28T10:00:00Z"
                }
                """
)
public record StockMovementResponse(
        @Schema(description = "Evento que originou o movimento.", example = "evt-001")
        String eventId,
        @Schema(
                description = "Tipo do movimento persistido no ledger.",
                example = "STOCK_ADJUSTED",
                allowableValues = {"STOCK_ADJUSTED", "ORDER_CREATED", "ORDER_CANCELLED"}
        )
        String movementType,
        @Schema(description = "Variacao aplicada ao saldo. Pode ser negativa para reserva de pedido.", example = "10")
        int quantityDelta,
        @Schema(description = "Saldo antes do movimento.", example = "0")
        int previousAvailable,
        @Schema(description = "Saldo apos o movimento.", example = "10")
        int currentAvailable,
        @Schema(description = "Motivo/auditoria registrada junto ao movimento.", example = "manual_adjustment")
        String reason,
        @Schema(description = "Instante original do evento em UTC.", example = "2026-05-28T10:00:00Z")
        Instant occurredAt
) {
}
