package com.gubee.stockreconciliation.adapter.out.persistence;

import com.gubee.stockreconciliation.adapter.out.persistence.repository.StockEventJpaRepository;
import com.gubee.stockreconciliation.domain.model.stock.StockEvent;
import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.port.out.StockLedgerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
class JpaStockLedgerAdapter implements StockLedgerPort {

    private final StockEventJpaRepository stockEventJpaRepository;
    private final StockEventPersistenceMapper stockEventPersistenceMapper;

    JpaStockLedgerAdapter(
            StockEventJpaRepository stockEventJpaRepository,
            StockEventPersistenceMapper stockEventPersistenceMapper
    ) {
        this.stockEventJpaRepository = stockEventJpaRepository;
        this.stockEventPersistenceMapper = stockEventPersistenceMapper;
    }

    @Override
    public Optional<StockEvent> findEventById(String eventId) {
        return stockEventJpaRepository.findByEventId(eventId)
                .map(stockEventPersistenceMapper::toDomain);
    }

    @Override
    public void append(StockEvent event) {
        stockEventJpaRepository.save(stockEventPersistenceMapper.toEntity(event));
    }

    @Override
    public List<StockEvent> findEventsByStockKey(StockKey stockKey) {
        return stockEventJpaRepository
                .findByAccountIdAndSkuOrderByOccurredAtAscIdAsc(stockKey.accountId(), stockKey.sku())
                .stream()
                .map(stockEventPersistenceMapper::toDomain)
                .toList();
    }
}
