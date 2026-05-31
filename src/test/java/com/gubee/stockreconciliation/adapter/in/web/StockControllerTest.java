package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.domain.model.MovementType;
import com.gubee.stockreconciliation.domain.model.StockBalance;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.model.StockMovement;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetStockHistoryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
@Import({StockRestMapper.class, RestExceptionHandler.class})
class StockControllerTest {

    private static final StockKey STOCK_KEY = new StockKey("account-001", "ABC-123");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetCurrentStockUseCase getCurrentStockUseCase;

    @MockBean
    private GetStockHistoryUseCase getStockHistoryUseCase;

    @Test
    void getsCurrentStock() throws Exception {
        when(getCurrentStockUseCase.getCurrentStock(STOCK_KEY))
                .thenReturn(Optional.of(new StockBalance(STOCK_KEY, 8)));

        mockMvc.perform(get("/api/v1/stocks/account-001/ABC-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("account-001"))
                .andExpect(jsonPath("$.sku").value("ABC-123"))
                .andExpect(jsonPath("$.available").value(8));
    }

    @Test
    void returnsNotFoundWhenStockDoesNotExist() throws Exception {
        when(getCurrentStockUseCase.getCurrentStock(STOCK_KEY)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/stocks/account-001/ABC-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getsStockHistory() throws Exception {
        when(getStockHistoryUseCase.getHistory(STOCK_KEY)).thenReturn(List.of(
                new StockMovement(
                        "evt-001",
                        STOCK_KEY,
                        MovementType.STOCK_ADJUSTED,
                        10,
                        0,
                        10,
                        "manual_adjustment",
                        Instant.parse("2026-05-28T10:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/v1/stocks/account-001/ABC-123/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-001"))
                .andExpect(jsonPath("$[0].movementType").value("STOCK_ADJUSTED"))
                .andExpect(jsonPath("$[0].currentAvailable").value(10));
    }
}
