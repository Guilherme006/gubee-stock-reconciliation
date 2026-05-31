package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.adapter.in.web.dto.StockBalanceResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.StockMovementResponse;
import com.gubee.stockreconciliation.domain.model.StockKey;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetStockHistoryUseCase;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
@Profile("!test")
class StockController {

    private final GetCurrentStockUseCase getCurrentStockUseCase;
    private final GetStockHistoryUseCase getStockHistoryUseCase;
    private final StockRestMapper stockRestMapper;

    StockController(
            GetCurrentStockUseCase getCurrentStockUseCase,
            GetStockHistoryUseCase getStockHistoryUseCase,
            StockRestMapper stockRestMapper
    ) {
        this.getCurrentStockUseCase = getCurrentStockUseCase;
        this.getStockHistoryUseCase = getStockHistoryUseCase;
        this.stockRestMapper = stockRestMapper;
    }

    @GetMapping("/{accountId}/{sku}")
    StockBalanceResponse getCurrentStock(@PathVariable String accountId, @PathVariable String sku) {
        var stockKey = new StockKey(accountId, sku);
        return getCurrentStockUseCase.getCurrentStock(stockKey)
                .map(stockRestMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
    }

    @GetMapping("/{accountId}/{sku}/history")
    List<StockMovementResponse> getHistory(@PathVariable String accountId, @PathVariable String sku) {
        var stockKey = new StockKey(accountId, sku);
        return getStockHistoryUseCase.getHistory(stockKey).stream()
                .map(stockRestMapper::toResponse)
                .toList();
    }
}
