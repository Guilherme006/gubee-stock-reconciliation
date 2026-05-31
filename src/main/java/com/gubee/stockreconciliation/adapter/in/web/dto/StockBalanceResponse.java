package com.gubee.stockreconciliation.adapter.in.web.dto;

public record StockBalanceResponse(
        String accountId,
        String sku,
        int available
) {
}
