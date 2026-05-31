package com.gubee.stockreconciliation.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gubee.stockreconciliation.adapter.out.persistence.entity.StockEventEntity;
import com.gubee.stockreconciliation.domain.model.StockEvent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Component
class StockEventPersistenceMapper {

    private static final String RECEIVED_STATUS = "RECEIVED";

    private final ObjectMapper objectMapper;

    StockEventPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    StockEventEntity toEntity(StockEvent event) {
        var payload = writePayload(event);
        return new StockEventEntity(
                event.eventId(),
                event.type().name(),
                event.accountId(),
                event.sku(),
                event.marketplace(),
                event.externalOrderId(),
                event.occurredAt(),
                Instant.now(),
                RECEIVED_STATUS,
                payload,
                sha256(payload),
                null
        );
    }

    StockEvent toDomain(StockEventEntity entity) {
        try {
            return objectMapper.readValue(entity.getPayload(), StockEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize stock event " + entity.getEventId(), exception);
        }
    }

    private String writePayload(StockEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize stock event " + event.eventId(), exception);
        }
    }

    private static String sha256(String payload) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
