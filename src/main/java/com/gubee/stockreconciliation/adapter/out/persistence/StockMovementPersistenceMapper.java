package com.gubee.stockreconciliation.adapter.out.persistence;

import com.gubee.stockreconciliation.adapter.out.persistence.entity.StockMovementEntity;
import com.gubee.stockreconciliation.domain.model.stock.MovementType;
import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.model.stock.StockMovement;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class StockMovementPersistenceMapper {

    StockMovementEntity toEntity(StockMovement movement) {
        return new StockMovementEntity(
                movement.eventId(),
                movement.stockKey().accountId(),
                movement.stockKey().sku(),
                movement.movementType().name(),
                movement.quantityDelta(),
                movement.previousAvailable(),
                movement.currentAvailable(),
                movement.reason(),
                movement.occurredAt(),
                Instant.now()
        );
    }

    StockMovement toDomain(StockMovementEntity entity) {
        return new StockMovement(
                entity.getEventId(),
                new StockKey(entity.getAccountId(), entity.getSku()),
                MovementType.valueOf(entity.getMovementType()),
                entity.getQuantityDelta(),
                entity.getPreviousAvailable(),
                entity.getCurrentAvailable(),
                entity.getReason(),
                entity.getOccurredAt()
        );
    }
}
