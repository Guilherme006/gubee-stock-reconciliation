package com.gubee.stockreconciliation.adapter.out.persistence;

import com.gubee.stockreconciliation.adapter.out.persistence.repository.StockProjectionJpaRepository;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationLockPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
@Profile("!test")
class JpaStockReconciliationLockAdapter implements StockReconciliationLockPort {

    private final StockProjectionJpaRepository stockProjectionJpaRepository;

    JpaStockReconciliationLockAdapter(StockProjectionJpaRepository stockProjectionJpaRepository) {
        this.stockProjectionJpaRepository = stockProjectionJpaRepository;
    }

    @Override
    @Transactional
    public <T> T withStockLock(StockKey stockKey, Supplier<T> operation) {
        ensureProjectionExists(stockKey);
        stockProjectionJpaRepository
                .findByAccountIdAndSkuForUpdate(stockKey.accountId(), stockKey.sku())
                .orElseThrow(() -> new IllegalStateException("Stock projection lock could not be acquired"));
        return operation.get();
    }

    private void ensureProjectionExists(StockKey stockKey) {
        stockProjectionJpaRepository.insertIgnore(stockKey.accountId(), stockKey.sku());
    }
}
