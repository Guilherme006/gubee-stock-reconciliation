package com.gubee.stockreconciliation.adapter.in.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gubee.stockreconciliation.adapter.in.kafka.dto.StockEventMessage;
import com.gubee.stockreconciliation.adapter.out.observability.StockEventMetrics;
import com.gubee.stockreconciliation.domain.model.processing.ProcessStockEventResult;
import com.gubee.stockreconciliation.domain.model.stock.StockEvent;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
class StockEventKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final KafkaStockEventMapper stockEventMapper;
    private final ProcessStockEventUseCase processStockEventUseCase;
    private final StockEventMetrics stockEventMetrics;

    StockEventKafkaListener(
            ObjectMapper objectMapper,
            KafkaStockEventMapper stockEventMapper,
            ProcessStockEventUseCase processStockEventUseCase,
            StockEventMetrics stockEventMetrics
    ) {
        this.objectMapper = objectMapper;
        this.stockEventMapper = stockEventMapper;
        this.processStockEventUseCase = processStockEventUseCase;
        this.stockEventMetrics = stockEventMetrics;
    }

    @KafkaListener(topics = "${stock-reconciliation.kafka.stock-events-topic}")
    void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        var event = parseEvent(record, acknowledgment);
        if (event == null) {
            return;
        }

        try {
            putEventMdc(event, record);
            var result = processStockEventUseCase.process(event);
            logProcessed(result);
            stockEventMetrics.recordProcessed(result.processedEvent().status(), "kafka");
            acknowledgment.acknowledge();
        } catch (RuntimeException exception) {
            log.error(
                    "stock_event_processing_failed topic={} partition={} offset={} key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    exception
            );
            throw exception;
        } finally {
            clearEventMdc();
        }
    }

    private StockEvent parseEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            var message = objectMapper.readValue(record.value(), StockEventMessage.class);
            return stockEventMapper.toDomain(message, record.offset());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn(
                    "stock_event_rejected topic={} partition={} offset={} key={} reason={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    exception.getMessage()
            );
            stockEventMetrics.recordRejected("kafka", exception.getClass().getSimpleName());
            acknowledgment.acknowledge();
            return null;
        }
    }

    private static void putEventMdc(StockEvent event, ConsumerRecord<String, String> record) {
        MDC.put("eventId", event.eventId());
        MDC.put("eventType", event.type().name());
        MDC.put("accountId", event.accountId());
        MDC.put("sku", event.sku());
        MDC.put("kafkaTopic", record.topic());
        MDC.put("kafkaPartition", String.valueOf(record.partition()));
        MDC.put("kafkaOffset", String.valueOf(record.offset()));
    }

    private static void clearEventMdc() {
        MDC.remove("eventId");
        MDC.remove("eventType");
        MDC.remove("accountId");
        MDC.remove("sku");
        MDC.remove("kafkaTopic");
        MDC.remove("kafkaPartition");
        MDC.remove("kafkaOffset");
    }

    private static void logProcessed(ProcessStockEventResult result) {
        log.info(
                "stock_event_processed status={} currentAvailable={} movements={}",
                result.processedEvent().status(),
                result.currentAvailable(),
                result.movements().size()
        );
    }
}
