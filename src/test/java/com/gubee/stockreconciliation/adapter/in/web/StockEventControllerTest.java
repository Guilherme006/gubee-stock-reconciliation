package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.domain.event.StockEventType;
import com.gubee.stockreconciliation.domain.model.ProcessStockEventResult;
import com.gubee.stockreconciliation.domain.model.ProcessedStockEvent;
import com.gubee.stockreconciliation.domain.model.ProcessingStatus;
import com.gubee.stockreconciliation.domain.model.StockBalance;
import com.gubee.stockreconciliation.domain.model.StockEvent;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.model.StockMovement;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockEventController.class)
@Import({StockRestMapper.class, RestExceptionHandler.class})
class StockEventControllerTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-05-28T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessStockEventUseCase processStockEventUseCase;

    @MockBean
    private GetProcessedStockEventUseCase getProcessedStockEventUseCase;

    @Test
    void processesStockEvent() throws Exception {
        var event = StockEvent.stockAdjusted(
                "evt-001",
                OCCURRED_AT,
                "account-001",
                "ABC-123",
                10,
                "manual_adjustment",
                1
        );
        when(processStockEventUseCase.process(any(StockEvent.class))).thenReturn(new ProcessStockEventResult(
                new ProcessedStockEvent(event, ProcessingStatus.APPLIED, "Stock adjusted"),
                10,
                List.of()
        ));

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-001",
                                  "type": "STOCK_ADJUSTED",
                                  "occurredAt": "2026-05-28T10:00:00Z",
                                  "accountId": "account-001",
                                  "sku": "ABC-123",
                                  "available": 10,
                                  "reason": "manual_adjustment"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.currentAvailable").value(10));
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "STOCK_ADJUSTED",
                                  "occurredAt": "2026-05-28T10:00:00Z",
                                  "accountId": "account-001",
                                  "sku": "ABC-123",
                                  "available": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void findsProcessedEvent() throws Exception {
        var event = new StockEvent(
                "evt-001",
                StockEventType.STOCK_SYNC_SENT,
                OCCURRED_AT,
                "account-001",
                "ABC-123",
                "MERCADO_LIVRE",
                null,
                null,
                null,
                8,
                null,
                1
        );
        when(getProcessedStockEventUseCase.getProcessedEvent("evt-001"))
                .thenReturn(Optional.of(new ProcessedStockEvent(event, ProcessingStatus.AUDITED, "audited")));

        mockMvc.perform(get("/api/v1/events/evt-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("evt-001"))
                .andExpect(jsonPath("$.status").value("AUDITED"));
    }

    @Test
    void returnsNotFoundWhenProcessedEventDoesNotExist() throws Exception {
        when(getProcessedStockEventUseCase.getProcessedEvent("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/events/missing"))
                .andExpect(status().isNotFound());
    }
}
