package com.gubee.stockreconciliation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"eventId", "type", "accountId", "sku", "status", "detail"})
@Schema(
        name = "ProcessedEventResponse",
        title = "Evento processado",
        description = "Consulta de um evento ja registrado no ledger de idempotencia.",
        example = """
                {
                  "eventId": "evt-001",
                  "type": "STOCK_ADJUSTED",
                  "accountId": "account-001",
                  "sku": "ABC-123",
                  "status": "APPLIED",
                  "detail": "Stock adjusted"
                }
                """
)
public record ProcessedEventResponse(
        @Schema(description = "Identificador global do evento.", example = "evt-001")
        String eventId,
        @Schema(
                description = "Tipo do evento recebido.",
                example = "STOCK_ADJUSTED",
                allowableValues = {
                        "STOCK_ADJUSTED",
                        "ORDER_CREATED",
                        "ORDER_CANCELLED",
                        "STOCK_SYNC_SENT",
                        "MARKETPLACE_STOCK_RESTORED"
                }
        )
        String type,
        @Schema(description = "Conta/tenant dono do estoque.", example = "account-001")
        String accountId,
        @Schema(description = "SKU reconciliado.", example = "ABC-123")
        String sku,
        @Schema(
                description = "Status final armazenado para o evento.",
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
        @Schema(description = "Detalhe legivel do processamento.", example = "Stock adjusted")
        String detail
) {
}
