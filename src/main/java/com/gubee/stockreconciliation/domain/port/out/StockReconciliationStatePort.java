package com.gubee.stockreconciliation.domain.port.out;

import com.gubee.stockreconciliation.domain.model.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.StockBalance;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.model.StockMovement;
import com.gubee.stockreconciliation.domain.model.StockReconciliationResult;

import java.util.List;
import java.util.Optional;

public interface StockReconciliationStatePort {

    void saveReconciliation(StockKey stockKey, StockReconciliationResult result);

    Optional<StockBalance> findCurrentStock(StockKey stockKey);

    List<StockMovement> findHistory(StockKey stockKey);

    Optional<ProcessedStockEvent> findProcessedEvent(String eventId);
}
