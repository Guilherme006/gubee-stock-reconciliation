package com.gubee.stockreconciliation.domain.model.stock;

import com.gubee.stockreconciliation.domain.event.StockEventType;
import com.gubee.stockreconciliation.domain.model.order.OrderKey;

import java.time.Instant;
import java.util.Objects;

public record StockEvent(
        String eventId,
        StockEventType type,
        Instant occurredAt,
        String accountId,
        String sku,
        String marketplace,
        String externalOrderId,
        Integer quantity,
        Integer available,
        Integer quantitySent,
        String reason,
        long receivedSequence
) {

    public StockEvent {
        eventId = requireText(eventId, "eventId");
        type = Objects.requireNonNull(type, "type must not be null");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        accountId = requireText(accountId, "accountId");
        sku = requireText(sku, "sku");

        validateByType(type, marketplace, externalOrderId, quantity, available, quantitySent);
    }

    public static StockEvent stockAdjusted(
            String eventId,
            Instant occurredAt,
            String accountId,
            String sku,
            int available,
            String reason,
            long receivedSequence
    ) {
        return new StockEvent(
                eventId,
                StockEventType.STOCK_ADJUSTED,
                occurredAt,
                accountId,
                sku,
                null,
                null,
                null,
                available,
                null,
                reason,
                receivedSequence
        );
    }

    public static StockEvent orderCreated(
            String eventId,
            Instant occurredAt,
            String marketplace,
            String accountId,
            String externalOrderId,
            String sku,
            int quantity,
            long receivedSequence
    ) {
        return orderEvent(
                eventId,
                StockEventType.ORDER_CREATED,
                occurredAt,
                marketplace,
                accountId,
                externalOrderId,
                sku,
                quantity,
                receivedSequence
        );
    }

    public static StockEvent orderCancelled(
            String eventId,
            Instant occurredAt,
            String marketplace,
            String accountId,
            String externalOrderId,
            String sku,
            int quantity,
            long receivedSequence
    ) {
        return orderEvent(
                eventId,
                StockEventType.ORDER_CANCELLED,
                occurredAt,
                marketplace,
                accountId,
                externalOrderId,
                sku,
                quantity,
                receivedSequence
        );
    }

    public static StockEvent stockSyncSent(
            String eventId,
            Instant occurredAt,
            String marketplace,
            String accountId,
            String sku,
            int quantitySent,
            long receivedSequence
    ) {
        return new StockEvent(
                eventId,
                StockEventType.STOCK_SYNC_SENT,
                occurredAt,
                accountId,
                sku,
                marketplace,
                null,
                null,
                null,
                quantitySent,
                null,
                receivedSequence
        );
    }

    public static StockEvent marketplaceStockRestored(
            String eventId,
            Instant occurredAt,
            String marketplace,
            String accountId,
            String externalOrderId,
            String sku,
            int quantity,
            long receivedSequence
    ) {
        return orderEvent(
                eventId,
                StockEventType.MARKETPLACE_STOCK_RESTORED,
                occurredAt,
                marketplace,
                accountId,
                externalOrderId,
                sku,
                quantity,
                receivedSequence
        );
    }

    public StockKey stockKey() {
        return new StockKey(accountId, sku);
    }

    public OrderKey orderKey() {
        return new OrderKey(accountId, marketplace, externalOrderId, sku);
    }

    public boolean samePayloadAs(StockEvent other) {
        return other != null
                && Objects.equals(eventId, other.eventId)
                && type == other.type
                && Objects.equals(occurredAt, other.occurredAt)
                && Objects.equals(accountId, other.accountId)
                && Objects.equals(sku, other.sku)
                && Objects.equals(marketplace, other.marketplace)
                && Objects.equals(externalOrderId, other.externalOrderId)
                && Objects.equals(quantity, other.quantity)
                && Objects.equals(available, other.available)
                && Objects.equals(quantitySent, other.quantitySent)
                && Objects.equals(reason, other.reason);
    }

    private static StockEvent orderEvent(
            String eventId,
            StockEventType type,
            Instant occurredAt,
            String marketplace,
            String accountId,
            String externalOrderId,
            String sku,
            int quantity,
            long receivedSequence
    ) {
        return new StockEvent(
                eventId,
                type,
                occurredAt,
                accountId,
                sku,
                marketplace,
                externalOrderId,
                quantity,
                null,
                null,
                null,
                receivedSequence
        );
    }

    private static void validateByType(
            StockEventType type,
            String marketplace,
            String externalOrderId,
            Integer quantity,
            Integer available,
            Integer quantitySent
    ) {
        switch (type) {
            case STOCK_ADJUSTED -> requireNonNegative(available, "available");
            case ORDER_CREATED, ORDER_CANCELLED, MARKETPLACE_STOCK_RESTORED -> {
                requireText(marketplace, "marketplace");
                requireText(externalOrderId, "externalOrderId");
                requirePositive(quantity, "quantity");
            }
            case STOCK_SYNC_SENT -> {
                requireText(marketplace, "marketplace");
                requireNonNegative(quantitySent, "quantitySent");
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static void requirePositive(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    private static void requireNonNegative(Integer value, String field) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
    }
}
