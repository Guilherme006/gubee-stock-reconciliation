package com.gubee.stockreconciliation.domain.port.out;

import com.gubee.stockreconciliation.domain.model.StockKey;

import java.util.function.Supplier;

public interface StockReconciliationLockPort {

    <T> T withStockLock(StockKey stockKey, Supplier<T> operation);
}
