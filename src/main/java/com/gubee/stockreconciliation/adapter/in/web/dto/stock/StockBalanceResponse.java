package com.gubee.stockreconciliation.adapter.in.web.dto.stock;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonPropertyOrder({"accountId", "sku", "available"})
@Schema(
        name = "StockBalanceResponse",
        title = "Saldo atual",
        description = "Projecao atual do saldo reconciliado para uma conta e SKU.",
        example = """
                {
                  "accountId": "account-001",
                  "sku": "ABC-123",
                  "available": 8
                }
                """
)
public record StockBalanceResponse(
        @Schema(description = "Conta/tenant dono do estoque.", example = "account-001")
        String accountId,
        @Schema(description = "SKU consultado.", example = "ABC-123")
        String sku,
        @Schema(description = "Quantidade disponivel apos reconciliacao.", example = "8")
        int available
) {
}
