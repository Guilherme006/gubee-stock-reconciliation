package com.gubee.stockreconciliation.adapter.in.web.dto.event;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gubee.stockreconciliation.adapter.in.web.dto.stock.StockMovementResponse;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonPropertyOrder({"eventId", "status", "detail", "currentAvailable", "movements"})
@Schema(
        name = "ProcessStockEventResponse",
        title = "Resultado do processamento",
        description = "Resumo do resultado idempotente produzido pelo processamento de um evento de estoque.",
        example = """
                {
                  "eventId": "evt-001",
                  "status": "APPLIED",
                  "detail": "Stock adjusted",
                  "currentAvailable": 10,
                  "movements": [
                    {
                      "eventId": "evt-001",
                      "movementType": "STOCK_ADJUSTED",
                      "quantityDelta": 10,
                      "previousAvailable": 0,
                      "currentAvailable": 10,
                      "reason": "manual_adjustment",
                      "occurredAt": "2026-05-28T10:00:00Z"
                    }
                  ]
                }
                """
)
public record ProcessStockEventResponse(
        @Schema(description = "Identificador do evento processado.", example = "evt-001")
        String eventId,
        @Schema(
                description = "Status final da reconciliacao.",
                example = "APPLIED",
                allowableValues = {
                        "APPLIED",
                        "AUDITED",
                        "IGNORED_DUPLICATE_EVENT",
                        "IGNORED_DUPLICATE_ORDER",
                        "REJECTED_DUPLICATE_EVENT_ID",
                        "REJECTED_INSUFFICIENT_STOCK",
                        "REJECTED_ORDER_NOT_FOUND",
                        "REJECTED_ORDER_ALREADY_CANCELLED",
                        "REJECTED_INVALID_CANCEL_QUANTITY"
                }
        )
        String status,
        @Schema(description = "Mensagem curta para auditoria e suporte.", example = "Stock adjusted")
        String detail,
        @Schema(description = "Saldo atual apos o processamento do evento.", example = "10")
        int currentAvailable,
        @ArraySchema(
                schema = @Schema(implementation = StockMovementResponse.class),
                arraySchema = @Schema(description = "Movimentos persistidos no ledger para este evento.")
        )
        List<StockMovementResponse> movements
) {
}
