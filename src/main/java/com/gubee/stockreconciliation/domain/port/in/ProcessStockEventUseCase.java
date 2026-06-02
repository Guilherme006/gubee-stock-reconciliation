package com.gubee.stockreconciliation.domain.port.in;

import com.gubee.stockreconciliation.domain.model.processing.ProcessStockEventResult;
import com.gubee.stockreconciliation.domain.model.stock.StockEvent;

public interface ProcessStockEventUseCase {

    ProcessStockEventResult process(StockEvent event);
}
