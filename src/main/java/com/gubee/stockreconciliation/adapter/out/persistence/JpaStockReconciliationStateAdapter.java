package com.gubee.stockreconciliation.adapter.out.persistence;

import com.gubee.stockreconciliation.adapter.out.persistence.entity.StockProjectionEntity;
import com.gubee.stockreconciliation.adapter.out.persistence.repository.StockEventJpaRepository;
import com.gubee.stockreconciliation.adapter.out.persistence.repository.StockMovementJpaRepository;
import com.gubee.stockreconciliation.adapter.out.persistence.repository.StockProjectionJpaRepository;
import com.gubee.stockreconciliation.domain.model.processing.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.processing.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.stock.StockBalance;
import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.model.stock.StockMovement;
import com.gubee.stockreconciliation.domain.model.stock.StockReconciliationResult;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationStatePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
class JpaStockReconciliationStateAdapter implements StockReconciliationStatePort {

    private final StockProjectionJpaRepository stockProjectionJpaRepository;
    private final StockMovementJpaRepository stockMovementJpaRepository;
    private final StockEventJpaRepository stockEventJpaRepository;
    private final StockEventPersistenceMapper stockEventPersistenceMapper;
    private final StockMovementPersistenceMapper stockMovementPersistenceMapper;

    JpaStockReconciliationStateAdapter(
            StockProjectionJpaRepository stockProjectionJpaRepository,
            StockMovementJpaRepository stockMovementJpaRepository,
            StockEventJpaRepository stockEventJpaRepository,
            StockEventPersistenceMapper stockEventPersistenceMapper,
            StockMovementPersistenceMapper stockMovementPersistenceMapper
    ) {
        this.stockProjectionJpaRepository = stockProjectionJpaRepository;
        this.stockMovementJpaRepository = stockMovementJpaRepository;
        this.stockEventJpaRepository = stockEventJpaRepository;
        this.stockEventPersistenceMapper = stockEventPersistenceMapper;
        this.stockMovementPersistenceMapper = stockMovementPersistenceMapper;
    }

    @Override
    public void saveReconciliation(StockKey stockKey, StockReconciliationResult result) {
        upsertProjection(stockKey, result.availableFor(stockKey));
        replaceMovements(stockKey, result.movements());
        updateProcessedEvents(result.processedEvents());
    }

    @Override
    public Optional<StockBalance> findCurrentStock(StockKey stockKey) {
        return stockProjectionJpaRepository.findByAccountIdAndSku(stockKey.accountId(), stockKey.sku())
                .map(entity -> new StockBalance(stockKey, entity.getAvailable()));
    }

    @Override
    public List<StockMovement> findHistory(StockKey stockKey) {
        return stockMovementJpaRepository
                .findByAccountIdAndSkuOrderByOccurredAtAscIdAsc(stockKey.accountId(), stockKey.sku())
                .stream()
                .map(stockMovementPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ProcessedStockEvent> findProcessedEvent(String eventId) {
        return stockEventJpaRepository.findByEventId(eventId)
                .map(entity -> new ProcessedStockEvent(
                        stockEventPersistenceMapper.toDomain(entity),
                        ProcessingStatus.valueOf(entity.getProcessingStatus()),
                        entity.getFailureReason()
                ));
    }

    private void upsertProjection(StockKey stockKey, int available) {
        var projection = stockProjectionJpaRepository
                .findByAccountIdAndSku(stockKey.accountId(), stockKey.sku())
                .orElseGet(() -> new StockProjectionEntity(stockKey.accountId(), stockKey.sku(), 0));
        projection.setAvailable(available);
        stockProjectionJpaRepository.save(projection);
    }

    private void replaceMovements(StockKey stockKey, List<StockMovement> movements) {
        stockMovementJpaRepository.deleteByAccountIdAndSku(stockKey.accountId(), stockKey.sku());
        stockMovementJpaRepository.saveAll(movements.stream()
                .map(stockMovementPersistenceMapper::toEntity)
                .toList());
    }

    private void updateProcessedEvents(List<ProcessedStockEvent> processedEvents) {
        processedEvents.forEach(processed -> stockEventJpaRepository
                .findByEventId(processed.event().eventId())
                .ifPresent(entity -> entity.markProcessed(
                        processed.status().name(),
                        processed.detail()
                )));
    }
}
