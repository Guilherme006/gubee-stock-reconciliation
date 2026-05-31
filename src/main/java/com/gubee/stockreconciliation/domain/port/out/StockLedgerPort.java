package com.gubee.stockreconciliation.domain.port.out;

import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.model.StockKey;

import java.util.List;
import java.util.Optional;

public interface StockLedgerPort {

    Optional<StockEvent> findEventById(String eventId);

    void append(StockEvent event);

    List<StockEvent> findEventsByStockKey(StockKey stockKey);
}
