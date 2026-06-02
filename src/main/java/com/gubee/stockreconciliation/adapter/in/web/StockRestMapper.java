package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.adapter.in.web.dto.event.ProcessStockEventResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.event.ProcessedEventResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.stock.StockBalanceResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.event.StockEventRequest;
import com.gubee.stockreconciliation.adapter.in.web.dto.stock.StockMovementResponse;
import com.gubee.stockreconciliation.domain.model.processing.ProcessStockEventResult;
import com.gubee.stockreconciliation.domain.model.processing.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.stock.StockBalance;
import com.gubee.stockreconciliation.domain.model.stock.StockEvent;
import com.gubee.stockreconciliation.domain.model.stock.StockMovement;
import org.springframework.stereotype.Component;

@Component
class StockRestMapper {

    StockEvent toDomain(StockEventRequest request) {
        var receivedSequence = System.currentTimeMillis();
        return switch (request.type()) {
            case STOCK_ADJUSTED -> StockEvent.stockAdjusted(
                    request.eventId(),
                    request.occurredAt(),
                    request.accountId(),
                    request.sku(),
                    requireValue(request.available(), "available"),
                    request.reason(),
                    receivedSequence
            );
            case ORDER_CREATED -> StockEvent.orderCreated(
                    request.eventId(),
                    request.occurredAt(),
                    request.marketplace(),
                    request.accountId(),
                    request.externalOrderId(),
                    request.sku(),
                    requireValue(request.quantity(), "quantity"),
                    receivedSequence
            );
            case ORDER_CANCELLED -> StockEvent.orderCancelled(
                    request.eventId(),
                    request.occurredAt(),
                    request.marketplace(),
                    request.accountId(),
                    request.externalOrderId(),
                    request.sku(),
                    requireValue(request.quantity(), "quantity"),
                    receivedSequence
            );
            case STOCK_SYNC_SENT -> StockEvent.stockSyncSent(
                    request.eventId(),
                    request.occurredAt(),
                    request.marketplace(),
                    request.accountId(),
                    request.sku(),
                    requireValue(request.quantitySent(), "quantitySent"),
                    receivedSequence
            );
            case MARKETPLACE_STOCK_RESTORED -> StockEvent.marketplaceStockRestored(
                    request.eventId(),
                    request.occurredAt(),
                    request.marketplace(),
                    request.accountId(),
                    request.externalOrderId(),
                    request.sku(),
                    requireValue(request.quantity(), "quantity"),
                    receivedSequence
            );
        };
    }

    ProcessStockEventResponse toResponse(ProcessStockEventResult result) {
        var processedEvent = result.processedEvent();
        return new ProcessStockEventResponse(
                processedEvent.event().eventId(),
                processedEvent.status().name(),
                processedEvent.detail(),
                result.currentAvailable(),
                result.movements().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    StockBalanceResponse toResponse(StockBalance balance) {
        return new StockBalanceResponse(
                balance.stockKey().accountId(),
                balance.stockKey().sku(),
                balance.available()
        );
    }

    StockMovementResponse toResponse(StockMovement movement) {
        return new StockMovementResponse(
                movement.eventId(),
                movement.movementType().name(),
                movement.quantityDelta(),
                movement.previousAvailable(),
                movement.currentAvailable(),
                movement.reason(),
                movement.occurredAt()
        );
    }

    ProcessedEventResponse toResponse(ProcessedStockEvent processedEvent) {
        var event = processedEvent.event();
        return new ProcessedEventResponse(
                event.eventId(),
                event.type().name(),
                event.accountId(),
                event.sku(),
                processedEvent.status().name(),
                processedEvent.detail()
        );
    }

    private static int requireValue(Integer value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required for this event type");
        }
        return value;
    }
}
