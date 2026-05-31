package com.gubee.stockreconciliation.domain.model;

public record OrderKey(String accountId, String marketplace, String externalOrderId, String sku) {

    public OrderKey {
        accountId = requireText(accountId, "accountId");
        marketplace = requireText(marketplace, "marketplace");
        externalOrderId = requireText(externalOrderId, "externalOrderId");
        sku = requireText(sku, "sku");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
