package com.gubee.stockreconciliation.adapter.out.persistence.repository;

import com.gubee.stockreconciliation.adapter.out.persistence.entity.StockEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockEventJpaRepository extends JpaRepository<StockEventEntity, Long> {

    Optional<StockEventEntity> findByEventId(String eventId);

    List<StockEventEntity> findByAccountIdAndSkuOrderByOccurredAtAscIdAsc(String accountId, String sku);
}
