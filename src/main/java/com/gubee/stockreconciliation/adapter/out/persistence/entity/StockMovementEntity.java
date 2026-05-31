package com.gubee.stockreconciliation.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Column(name = "sku", nullable = false, length = 120)
    private String sku;

    @Column(name = "movement_type", nullable = false, length = 60)
    private String movementType;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "previous_available", nullable = false)
    private int previousAvailable;

    @Column(name = "current_available", nullable = false)
    private int currentAvailable;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected StockMovementEntity() {
    }

    public StockMovementEntity(
            String eventId,
            String accountId,
            String sku,
            String movementType,
            int quantityDelta,
            int previousAvailable,
            int currentAvailable,
            String reason,
            Instant occurredAt,
            Instant processedAt
    ) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.sku = sku;
        this.movementType = movementType;
        this.quantityDelta = quantityDelta;
        this.previousAvailable = previousAvailable;
        this.currentAvailable = currentAvailable;
        this.reason = reason;
        this.occurredAt = occurredAt;
        this.processedAt = processedAt;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSku() {
        return sku;
    }

    public String getMovementType() {
        return movementType;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public int getPreviousAvailable() {
        return previousAvailable;
    }

    public int getCurrentAvailable() {
        return currentAvailable;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
