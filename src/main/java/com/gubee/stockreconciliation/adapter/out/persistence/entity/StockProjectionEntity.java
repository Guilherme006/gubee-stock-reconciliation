package com.gubee.stockreconciliation.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_projections")
public class StockProjectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Column(name = "sku", nullable = false, length = 120)
    private String sku;

    @Column(name = "available", nullable = false)
    private int available;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected StockProjectionEntity() {
    }

    public StockProjectionEntity(String accountId, String sku, int available) {
        this.accountId = accountId;
        this.sku = sku;
        this.available = available;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }
}
