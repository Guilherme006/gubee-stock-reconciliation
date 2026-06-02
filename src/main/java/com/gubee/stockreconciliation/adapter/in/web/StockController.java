package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.adapter.in.web.dto.stock.StockBalanceResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.stock.StockMovementResponse;
import com.gubee.stockreconciliation.domain.model.stock.StockKey;
import com.gubee.stockreconciliation.domain.port.in.GetCurrentStockUseCase;
import com.gubee.stockreconciliation.domain.port.in.GetStockHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stocks", description = "Consultas do saldo reconciliado e historico de movimentos.")
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
    @Operation(summary = "Consulta o saldo atual reconciliado")
    @ApiResponse(responseCode = "200", description = "Saldo encontrado")
    @ApiResponse(responseCode = "404", description = "Saldo nao encontrado")
    StockBalanceResponse getCurrentStock(@PathVariable String accountId, @PathVariable String sku) {
        var stockKey = new StockKey(accountId, sku);
        return getCurrentStockUseCase.getCurrentStock(stockKey)
                .map(stockRestMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
    }

    @GetMapping("/{accountId}/{sku}/history")
    @Operation(summary = "Lista o historico de movimentos de estoque")
    @ApiResponse(responseCode = "200", description = "Historico retornado")
    List<StockMovementResponse> getHistory(@PathVariable String accountId, @PathVariable String sku) {
        var stockKey = new StockKey(accountId, sku);
        return getStockHistoryUseCase.getHistory(stockKey).stream()
                .map(stockRestMapper::toResponse)
                .toList();
    }
}
