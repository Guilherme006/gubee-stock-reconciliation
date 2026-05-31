package com.gubee.stockreconciliation.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stock_events")
public class StockEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Column(name = "sku", nullable = false, length = 120)
    private String sku;

    @Column(name = "marketplace", length = 80)
    private String marketplace;

    @Column(name = "external_order_id", length = 120)
    private String externalOrderId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processing_status", nullable = false, length = 40)
    private String processingStatus;

    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Column(name = "payload_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String payloadHash;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    protected StockEventEntity() {
    }

    public StockEventEntity(
            String eventId,
            String eventType,
            String accountId,
            String sku,
            String marketplace,
            String externalOrderId,
            Instant occurredAt,
            Instant receivedAt,
            String processingStatus,
            String payload,
            String payloadHash,
            String failureReason
    ) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.accountId = accountId;
        this.sku = sku;
        this.marketplace = marketplace;
        this.externalOrderId = externalOrderId;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
        this.processingStatus = processingStatus;
        this.payload = payload;
        this.payloadHash = payloadHash;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSku() {
        return sku;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public String getPayload() {
        return payload;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markProcessed(String processingStatus, String failureReason) {
        this.processingStatus = processingStatus;
        this.failureReason = failureReason;
    }
}
