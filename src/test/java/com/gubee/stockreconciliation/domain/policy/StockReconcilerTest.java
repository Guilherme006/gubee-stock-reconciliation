package com.gubee.stockreconciliation.domain.policy;

import com.gubee.stockreconciliation.domain.model.MovementType;
import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.model.StockKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StockReconcilerTest {

    private static final Instant BASE_TIME = Instant.parse("2026-05-28T10:00:00Z");
    private static final StockKey ACCOUNT_001_ABC_123 = new StockKey("account-001", "ABC-123");

    private final StockReconciler reconciler = new StockReconciler();

    @Test
    void stockAdjustedDefinesInitialStock() {
        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(10);
        assertThat(result.movements()).singleElement()
                .satisfies(movement -> {
                    assertThat(movement.movementType()).isEqualTo(MovementType.STOCK_ADJUSTED);
                    assertThat(movement.previousAvailable()).isZero();
                    assertThat(movement.currentAvailable()).isEqualTo(10);
                });
    }

    @Test
    void orderCreatedDecreasesStock() {
        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1),
                orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 2)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(8);
    }

    @Test
    void orderCancelledRestoresStock() {
        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1),
                orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 2),
                orderCancelled("evt-003", 2, "account-001", "ABC-123", "ML-123456", 2, 3)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(10);
        assertThat(result.movements()).extracting("movementType")
                .containsExactly(MovementType.STOCK_ADJUSTED, MovementType.ORDER_CREATED, MovementType.ORDER_CANCELLED);
    }

    @Test
    void duplicateEventIdDoesNotApplyTwice() {
        var event = orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 2);
        var duplicated = orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 3);

        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1),
                event,
                duplicated
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(8);
        assertThat(result.processedEvents()).extracting("status")
                .contains(ProcessingStatus.IGNORED_DUPLICATE_EVENT);
    }

    @Test
    void sameLogicalOrderWithDifferentEventIdDoesNotDecreaseStockTwice() {
        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1),
                orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 2),
                orderCreated("evt-003", 2, "account-001", "ABC-123", "ML-123456", 2, 3)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(8);
        assertThat(result.processedEvents()).extracting("status")
                .contains(ProcessingStatus.IGNORED_DUPLICATE_ORDER);
    }

    @Test
    void eventsAreAppliedByOccurrenceTimeInsteadOfArrivalOrder() {
        var result = reconciler.reconcile(List.of(
                orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 1),
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 2)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(8);
        assertThat(result.movements()).extracting("eventId")
                .containsExactly("evt-001", "evt-002");
    }

    @Test
    void sameSkuInDifferentAccountsHasIndependentStock() {
        var account002 = new StockKey("account-002", "ABC-123");

        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1),
                stockAdjusted("evt-002", 0, "account-002", "ABC-123", 3, 2),
                orderCreated("evt-003", 1, "account-001", "ABC-123", "ML-123456", 2, 3)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(8);
        assertThat(result.availableFor(account002)).isEqualTo(3);
    }

    @Test
    void orderCreatedCannotMakeStockNegative() {
        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 1, 1),
                orderCreated("evt-002", 1, "account-001", "ABC-123", "ML-123456", 2, 2)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(1);
        assertThat(result.processedEvents()).extracting("status")
                .contains(ProcessingStatus.REJECTED_INSUFFICIENT_STOCK);
    }

    @Test
    void marketplaceEventsAreAuditedWithoutChangingCanonicalStock() {
        var result = reconciler.reconcile(List.of(
                stockAdjusted("evt-001", 0, "account-001", "ABC-123", 10, 1),
                StockEvent.stockSyncSent("evt-002", BASE_TIME.plusSeconds(1), "MERCADO_LIVRE", "account-001", "ABC-123", 8, 2),
                StockEvent.marketplaceStockRestored("evt-003", BASE_TIME.plusSeconds(2), "MERCADO_LIVRE", "account-001", "ML-123456", "ABC-123", 2, 3)
        ));

        assertThat(result.availableFor(ACCOUNT_001_ABC_123)).isEqualTo(10);
        assertThat(result.processedEvents()).extracting("status")
                .contains(ProcessingStatus.AUDITED, ProcessingStatus.AUDITED);
    }

    private static StockEvent stockAdjusted(
            String eventId,
            long secondsAfterBase,
            String accountId,
            String sku,
            int available,
            long receivedSequence
    ) {
        return StockEvent.stockAdjusted(
                eventId,
                BASE_TIME.plusSeconds(secondsAfterBase),
                accountId,
                sku,
                available,
                "manual_adjustment",
                receivedSequence
        );
    }

    private static StockEvent orderCreated(
            String eventId,
            long secondsAfterBase,
            String accountId,
            String sku,
            String externalOrderId,
            int quantity,
            long receivedSequence
    ) {
        return StockEvent.orderCreated(
                eventId,
                BASE_TIME.plusSeconds(secondsAfterBase),
                "MERCADO_LIVRE",
                accountId,
                externalOrderId,
                sku,
                quantity,
                receivedSequence
        );
    }

    private static StockEvent orderCancelled(
            String eventId,
            long secondsAfterBase,
            String accountId,
            String sku,
            String externalOrderId,
            int quantity,
            long receivedSequence
    ) {
        return StockEvent.orderCancelled(
                eventId,
                BASE_TIME.plusSeconds(secondsAfterBase),
                "MERCADO_LIVRE",
                accountId,
                externalOrderId,
                sku,
                quantity,
                receivedSequence
        );
    }
}
