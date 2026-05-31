package com.gubee.stockreconciliation.domain.port.in;

import com.gubee.stockreconciliation.domain.model.ProcessStockEventResult;
import com.gubee.stockreconciliation.domain.model.StockEvent;

public interface ProcessStockEventUseCase {

    ProcessStockEventResult process(StockEvent event);
}
