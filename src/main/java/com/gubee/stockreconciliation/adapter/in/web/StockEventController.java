package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.adapter.in.web.dto.ProcessStockEventResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.ProcessedEventResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.StockEventRequest;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/events")
@Profile("!test")
class StockEventController {

    private final ProcessStockEventUseCase processStockEventUseCase;
    private final GetProcessedStockEventUseCase getProcessedStockEventUseCase;
    private final StockRestMapper stockRestMapper;

    StockEventController(
            ProcessStockEventUseCase processStockEventUseCase,
            GetProcessedStockEventUseCase getProcessedStockEventUseCase,
            StockRestMapper stockRestMapper
    ) {
        this.processStockEventUseCase = processStockEventUseCase;
        this.getProcessedStockEventUseCase = getProcessedStockEventUseCase;
        this.stockRestMapper = stockRestMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    ProcessStockEventResponse process(@Valid @RequestBody StockEventRequest request) {
        var event = stockRestMapper.toDomain(request);
        return stockRestMapper.toResponse(processStockEventUseCase.process(event));
    }

    @GetMapping("/{eventId}")
    ProcessedEventResponse findProcessedEvent(@PathVariable String eventId) {
        return getProcessedStockEventUseCase.getProcessedEvent(eventId)
                .map(stockRestMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}
