package com.gubee.stockreconciliation.adapter.out.observability;

import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class StockEventMetrics {

    private final MeterRegistry meterRegistry;

    public StockEventMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordProcessed(ProcessingStatus status, String source) {
        meterRegistry.counter(
                "stock.events.processed",
                "status", status.name(),
                "source", source
        ).increment();
    }

    public void recordRejected(String source, String reason) {
        meterRegistry.counter(
                "stock.events.rejected",
                "source", source,
                "reason", normalizeReason(reason)
        ).increment();
    }

    public void recordDeadLetter(String topic) {
        meterRegistry.counter(
                "stock.events.dead_letter",
                "topic", topic
        ).increment();
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        return reason.length() > 48 ? reason.substring(0, 48) : reason;
    }
}
