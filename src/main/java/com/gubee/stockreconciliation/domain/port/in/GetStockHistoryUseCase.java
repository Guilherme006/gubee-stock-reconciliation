package com.gubee.stockreconciliation.domain.port.in;

import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.model.stock.StockMovement;

import java.util.List;

public interface GetStockHistoryUseCase {

    List<StockMovement> getHistory(StockKey stockKey);
}
