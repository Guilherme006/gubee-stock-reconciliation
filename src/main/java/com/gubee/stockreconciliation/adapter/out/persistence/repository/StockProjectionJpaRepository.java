package com.gubee.stockreconciliation.adapter.out.persistence.repository;

import com.gubee.stockreconciliation.adapter.out.persistence.entity.StockProjectionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockProjectionJpaRepository extends JpaRepository<StockProjectionEntity, Long> {

    Optional<StockProjectionEntity> findByAccountIdAndSku(String accountId, String sku);

    @Modifying
    @Query(value = """
            insert ignore into stock_projections (account_id, sku, available)
            values (:accountId, :sku, 0)
            """, nativeQuery = true)
    void insertIgnore(
            @Param("accountId") String accountId,
            @Param("sku") String sku
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select projection
            from StockProjectionEntity projection
            where projection.accountId = :accountId and projection.sku = :sku
            """)
    Optional<StockProjectionEntity> findByAccountIdAndSkuForUpdate(
            @Param("accountId") String accountId,
            @Param("sku") String sku
    );
}
