package com.gubee.stockreconciliation.domain.port.in;

import com.gubee.stockreconciliation.domain.model.processing.ProcessedStockEvent;

import java.util.Optional;

public interface GetProcessedStockEventUseCase {

    Optional<ProcessedStockEvent> getProcessedEvent(String eventId);
}
