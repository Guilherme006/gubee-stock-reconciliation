package com.gubee.stockreconciliation.domain.policy;

import com.gubee.stockreconciliation.domain.model.MovementType;
import com.gubee.stockreconciliation.domain.model.OrderKey;
import com.gubee.stockreconciliation.domain.model.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.model.StockMovement;
import com.gubee.stockreconciliation.domain.model.StockReconciliationResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockReconciler {

    public StockReconciliationResult reconcile(List<StockEvent> events) {
        var balances = new LinkedHashMap<StockKey, Integer>();
        var movements = new ArrayList<StockMovement>();
        var processedEvents = new ArrayList<ProcessedStockEvent>();
        var processedByEventId = new HashMap<String, StockEvent>();
        var reservations = new HashMap<OrderKey, OrderReservation>();

        sorted(events).forEach(event -> {
            var previousEvent = processedByEventId.putIfAbsent(event.eventId(), event);
            if (previousEvent != null) {
                processedEvents.add(processDuplicateEvent(event, previousEvent));
                return;
            }

            switch (event.type()) {
                case STOCK_ADJUSTED -> applyStockAdjusted(event, balances, movements, processedEvents);
                case ORDER_CREATED -> applyOrderCreated(event, balances, movements, processedEvents, reservations);
                case ORDER_CANCELLED -> applyOrderCancelled(event, balances, movements, processedEvents, reservations);
                case STOCK_SYNC_SENT, MARKETPLACE_STOCK_RESTORED -> processedEvents.add(new ProcessedStockEvent(
                        event,
                        ProcessingStatus.AUDITED,
                        "Event audited without changing canonical stock"
                ));
            }
        });

        return new StockReconciliationResult(balances, movements, processedEvents);
    }

    private static List<StockEvent> sorted(List<StockEvent> events) {
        return events.stream()
                .sorted(Comparator
                        .comparing(StockEvent::occurredAt)
                        .thenComparingLong(StockEvent::receivedSequence))
                .toList();
    }

    private static ProcessedStockEvent processDuplicateEvent(StockEvent event, StockEvent previousEvent) {
        if (event.samePayloadAs(previousEvent)) {
            return new ProcessedStockEvent(
                    event,
                    ProcessingStatus.IGNORED_DUPLICATE_EVENT,
                    "Event id was already processed with the same payload"
            );
        }

        return new ProcessedStockEvent(
                event,
                ProcessingStatus.REJECTED_DUPLICATE_EVENT_ID,
                "Event id was already processed with a different payload"
        );
    }

    private static void applyStockAdjusted(
            StockEvent event,
            Map<StockKey, Integer> balances,
            List<StockMovement> movements,
            List<ProcessedStockEvent> processedEvents
    ) {
        var stockKey = event.stockKey();
        var previousAvailable = balances.getOrDefault(stockKey, 0);
        var currentAvailable = event.available();

        balances.put(stockKey, currentAvailable);
        movements.add(new StockMovement(
                event.eventId(),
                stockKey,
                MovementType.STOCK_ADJUSTED,
                currentAvailable - previousAvailable,
                previousAvailable,
                currentAvailable,
                event.reason(),
                event.occurredAt()
        ));
        processedEvents.add(new ProcessedStockEvent(event, ProcessingStatus.APPLIED, "Stock adjusted"));
    }

    private static void applyOrderCreated(
            StockEvent event,
            Map<StockKey, Integer> balances,
            List<StockMovement> movements,
            List<ProcessedStockEvent> processedEvents,
            Map<OrderKey, OrderReservation> reservations
    ) {
        var orderKey = event.orderKey();
        if (reservations.containsKey(orderKey)) {
            processedEvents.add(new ProcessedStockEvent(
                    event,
                    ProcessingStatus.IGNORED_DUPLICATE_ORDER,
                    "Logical order was already created"
            ));
            return;
        }

        var stockKey = event.stockKey();
        var previousAvailable = balances.getOrDefault(stockKey, 0);
        if (previousAvailable < event.quantity()) {
            processedEvents.add(new ProcessedStockEvent(
                    event,
                    ProcessingStatus.REJECTED_INSUFFICIENT_STOCK,
                    "Order would make stock negative"
            ));
            return;
        }

        var currentAvailable = previousAvailable - event.quantity();
        balances.put(stockKey, currentAvailable);
        reservations.put(orderKey, new OrderReservation(event.quantity(), false));
        movements.add(new StockMovement(
                event.eventId(),
                stockKey,
                MovementType.ORDER_CREATED,
                -event.quantity(),
                previousAvailable,
                currentAvailable,
                "order_created",
                event.occurredAt()
        ));
        processedEvents.add(new ProcessedStockEvent(event, ProcessingStatus.APPLIED, "Order created"));
    }

    private static void applyOrderCancelled(
            StockEvent event,
            Map<StockKey, Integer> balances,
            List<StockMovement> movements,
            List<ProcessedStockEvent> processedEvents,
            Map<OrderKey, OrderReservation> reservations
    ) {
        var orderKey = event.orderKey();
        var reservation = reservations.get(orderKey);
        if (reservation == null) {
            processedEvents.add(new ProcessedStockEvent(
                    event,
                    ProcessingStatus.REJECTED_ORDER_NOT_FOUND,
                    "Cancellation does not have a corresponding created order"
            ));
            return;
        }
        if (reservation.cancelled()) {
            processedEvents.add(new ProcessedStockEvent(
                    event,
                    ProcessingStatus.REJECTED_ORDER_ALREADY_CANCELLED,
                    "Order was already cancelled"
            ));
            return;
        }
        if (reservation.quantity() != event.quantity()) {
            processedEvents.add(new ProcessedStockEvent(
                    event,
                    ProcessingStatus.REJECTED_INVALID_CANCEL_QUANTITY,
                    "Cancellation quantity does not match created order quantity"
            ));
            return;
        }

        var stockKey = event.stockKey();
        var previousAvailable = balances.getOrDefault(stockKey, 0);
        var currentAvailable = previousAvailable + reservation.quantity();

        balances.put(stockKey, currentAvailable);
        reservations.put(orderKey, reservation.cancel());
        movements.add(new StockMovement(
                event.eventId(),
                stockKey,
                MovementType.ORDER_CANCELLED,
                reservation.quantity(),
                previousAvailable,
                currentAvailable,
                "order_cancelled",
                event.occurredAt()
        ));
        processedEvents.add(new ProcessedStockEvent(event, ProcessingStatus.APPLIED, "Order cancelled"));
    }

    private record OrderReservation(int quantity, boolean cancelled) {

        private OrderReservation cancel() {
            return new OrderReservation(quantity, true);
        }
    }
}
