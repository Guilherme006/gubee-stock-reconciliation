package com.gubee.stockreconciliation.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Saldo atual reconciliado de uma conta e SKU.")
public record StockBalanceResponse(
        @Schema(example = "account-001")
        String accountId,
        @Schema(example = "ABC-123")
        String sku,
        @Schema(example = "8")
        int available
) {
}
