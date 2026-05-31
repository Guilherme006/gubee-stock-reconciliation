package com.gubee.stockreconciliation.domain.event;

public enum StockEventType {
    STOCK_ADJUSTED,
    ORDER_CREATED,
    ORDER_CANCELLED,
    STOCK_SYNC_SENT,
    MARKETPLACE_STOCK_RESTORED
}
