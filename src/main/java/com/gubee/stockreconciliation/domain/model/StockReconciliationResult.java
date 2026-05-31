package com.gubee.stockreconciliation.domain.model;

import java.util.List;
import java.util.Map;

public record StockReconciliationResult(
        Map<StockKey, Integer> balances,
        List<StockMovement> movements,
        List<ProcessedStockEvent> processedEvents
) {

    public StockReconciliationResult {
        balances = Map.copyOf(balances);
        movements = List.copyOf(movements);
        processedEvents = List.copyOf(processedEvents);
    }

    public int availableFor(StockKey stockKey) {
        return balances.getOrDefault(stockKey, 0);
    }
}
