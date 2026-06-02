package com.gubee.stockreconciliation.domain.port.out;

import com.gubee.stockreconciliation.domain.model.processing.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.stock.StockBalance;
import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.model.stock.StockMovement;
import com.gubee.stockreconciliation.domain.model.stock.StockReconciliationResult;

import java.util.List;
import java.util.Optional;

public interface StockReconciliationStatePort {

    void saveReconciliation(StockKey stockKey, StockReconciliationResult result);

    Optional<StockBalance> findCurrentStock(StockKey stockKey);

    List<StockMovement> findHistory(StockKey stockKey);

    Optional<ProcessedStockEvent> findProcessedEvent(String eventId);
}
