package com.gubee.stockreconciliation.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.gubee.stockreconciliation.domain.event.StockEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@JsonPropertyOrder({
        "eventId",
        "type",
        "occurredAt",
        "accountId",
        "sku",
        "marketplace",
        "externalOrderId",
        "quantity",
        "available",
        "quantitySent",
        "reason"
})
@Schema(
        name = "StockEventRequest",
        title = "Evento de estoque",
        description = """
                Evento operacional usado para reconciliar o estoque.
                Cada eventId deve ser unico e idempotente. Os campos numericos obrigatorios dependem do tipo do evento:
                ORDER_CREATED usa quantity; ORDER_CANCELLED usa quantity; STOCK_ADJUSTED usa available; STOCK_SYNC_SENT usa quantitySent.
                """,
        example = """
                {
                  "eventId": "evt-001",
                  "type": "STOCK_ADJUSTED",
                  "occurredAt": "2026-05-28T10:00:00Z",
                  "accountId": "account-001",
                  "sku": "ABC-123",
                  "marketplace": "MERCADO_LIVRE",
                  "available": 10,
                  "reason": "manual_adjustment"
                }
                """
)
public record StockEventRequest(
        @Schema(
                description = "Identificador global do evento. Usado para idempotencia e rejeicao de duplicidade divergente.",
                example = "evt-001",
                minLength = 1,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String eventId,
        @Schema(
                description = "Tipo do evento de estoque.",
                example = "STOCK_ADJUSTED",
                implementation = StockEventType.class,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull StockEventType type,
        @Schema(
                description = "Instante em UTC em que o evento ocorreu na origem.",
                example = "2026-05-28T10:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull Instant occurredAt,
        @Schema(
                description = "Conta/tenant que possui o estoque.",
                example = "account-001",
                minLength = 1,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String accountId,
        @Schema(
                description = "SKU reconciliado dentro da conta.",
                example = "ABC-123",
                minLength = 1,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String sku,
        @Schema(description = "Canal/marketplace de origem do evento.", example = "MERCADO_LIVRE")
        String marketplace,
        @Schema(description = "Identificador do pedido no canal externo. Relevante para eventos de pedido.", example = "ML-123456")
        String externalOrderId,
        @Schema(description = "Quantidade do pedido para ORDER_CREATED e ORDER_CANCELLED.", example = "2", minimum = "1")
        Integer quantity,
        @Schema(description = "Saldo absoluto informado por eventos STOCK_ADJUSTED.", example = "10", minimum = "0")
        Integer available,
        @Schema(description = "Quantidade enviada ao marketplace em STOCK_SYNC_SENT.", example = "8", minimum = "0")
        Integer quantitySent,
        @Schema(description = "Motivo legivel para auditoria do movimento.", example = "manual_adjustment")
        String reason
) {
}
