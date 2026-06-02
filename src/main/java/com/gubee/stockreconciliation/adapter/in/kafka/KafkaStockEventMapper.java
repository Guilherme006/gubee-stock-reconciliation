package com.gubee.stockreconciliation.adapter.in.kafka;

import com.gubee.stockreconciliation.adapter.in.kafka.dto.StockEventMessage;
import com.gubee.stockreconciliation.domain.model.stock.StockEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
class KafkaStockEventMapper {

    StockEvent toDomain(StockEventMessage message, long receivedSequence) {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(message.type(), "type must not be null");

        return switch (message.type()) {
            case STOCK_ADJUSTED -> StockEvent.stockAdjusted(
                    message.eventId(),
                    message.occurredAt(),
                    message.accountId(),
                    message.sku(),
                    requireValue(message.available(), "available"),
                    message.reason(),
                    receivedSequence
            );
            case ORDER_CREATED -> StockEvent.orderCreated(
                    message.eventId(),
                    message.occurredAt(),
                    message.marketplace(),
                    message.accountId(),
                    message.externalOrderId(),
                    message.sku(),
                    requireValue(message.quantity(), "quantity"),
                    receivedSequence
            );
            case ORDER_CANCELLED -> StockEvent.orderCancelled(
                    message.eventId(),
                    message.occurredAt(),
                    message.marketplace(),
                    message.accountId(),
                    message.externalOrderId(),
                    message.sku(),
                    requireValue(message.quantity(), "quantity"),
                    receivedSequence
            );
            case STOCK_SYNC_SENT -> StockEvent.stockSyncSent(
                    message.eventId(),
                    message.occurredAt(),
                    message.marketplace(),
                    message.accountId(),
                    message.sku(),
                    requireValue(message.quantitySent(), "quantitySent"),
                    receivedSequence
            );
            case MARKETPLACE_STOCK_RESTORED -> StockEvent.marketplaceStockRestored(
                    message.eventId(),
                    message.occurredAt(),
                    message.marketplace(),
                    message.accountId(),
                    message.externalOrderId(),
                    message.sku(),
                    requireValue(message.quantity(), "quantity"),
                    receivedSequence
            );
        };
    }

    private static int requireValue(Integer value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required for this event type");
        }
        return value;
    }
}
