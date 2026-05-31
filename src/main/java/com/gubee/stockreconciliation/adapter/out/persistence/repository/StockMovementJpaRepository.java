package com.gubee.stockreconciliation.adapter.out.persistence.repository;

import com.gubee.stockreconciliation.adapter.out.persistence.entity.StockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, Long> {

    List<StockMovementEntity> findByAccountIdAndSkuOrderByOccurredAtAscIdAsc(String accountId, String sku);

    void deleteByAccountIdAndSku(String accountId, String sku);
}
