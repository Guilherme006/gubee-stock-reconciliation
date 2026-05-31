package com.gubee.stockreconciliation.domain.model;

import java.util.Objects;

public record StockBalance(StockKey stockKey, int available) {

    public StockBalance {
        stockKey = Objects.requireNonNull(stockKey, "stockKey must not be null");
        if (available < 0) {
            throw new IllegalArgumentException("available must be non-negative");
        }
    }
}
