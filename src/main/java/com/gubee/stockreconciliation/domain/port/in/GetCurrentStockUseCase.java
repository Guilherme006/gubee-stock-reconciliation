package com.gubee.stockreconciliation.domain.port.in;

import com.gubee.stockreconciliation.domain.model.StockBalance;
import com.gubee.stockreconciliation.domain.model.StockKey;

import java.util.Optional;

public interface GetCurrentStockUseCase {

    Optional<StockBalance> getCurrentStock(StockKey stockKey);
}
