package com.gubee.stockreconciliation.domain.port.in;

import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.model.StockMovement;

import java.util.List;

public interface GetStockHistoryUseCase {

    List<StockMovement> getHistory(StockKey stockKey);
}
