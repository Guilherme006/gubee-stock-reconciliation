package com.gubee.stockreconciliation.domain.model.stock;

public record StockKey(String accountId, String sku) {

    public StockKey {
        accountId = requireText(accountId, "accountId");
        sku = requireText(sku, "sku");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return accountId + ":" + sku;
    }
}
