package com.gubee.stockreconciliation.adapter.in.web;

import com.gubee.stockreconciliation.adapter.in.web.dto.event.ProcessStockEventResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.event.ProcessedEventResponse;
import com.gubee.stockreconciliation.adapter.in.web.dto.event.StockEventRequest;
import com.gubee.stockreconciliation.adapter.out.observability.StockEventMetrics;
import com.gubee.stockreconciliation.domain.port.in.GetProcessedStockEventUseCase;
import com.gubee.stockreconciliation.domain.port.in.ProcessStockEventUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stock events", description = "Processamento e auditoria de eventos de estoque.")
class StockEventController {

    private final ProcessStockEventUseCase processStockEventUseCase;
    private final GetProcessedStockEventUseCase getProcessedStockEventUseCase;
    private final StockRestMapper stockRestMapper;
    private final StockEventMetrics stockEventMetrics;

    StockEventController(
            ProcessStockEventUseCase processStockEventUseCase,
            GetProcessedStockEventUseCase getProcessedStockEventUseCase,
            StockRestMapper stockRestMapper,
            StockEventMetrics stockEventMetrics
    ) {
        this.processStockEventUseCase = processStockEventUseCase;
        this.getProcessedStockEventUseCase = getProcessedStockEventUseCase;
        this.stockRestMapper = stockRestMapper;
        this.stockEventMetrics = stockEventMetrics;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Processa um evento de estoque",
            description = "Endpoint protegido por Basic Auth para processar eventos operacionais manualmente.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponse(responseCode = "202", description = "Evento aceito e processado")
    @ApiResponse(
            responseCode = "400",
            description = "Payload invalido",
            content = @Content(examples = @ExampleObject(value = """
                    {"title":"Invalid request","status":400,"detail":"Request validation failed"}
                    """))
    )
    @ApiResponse(responseCode = "401", description = "Credenciais ausentes ou invalidas")
    ProcessStockEventResponse process(@Valid @RequestBody StockEventRequest request) {
        var event = stockRestMapper.toDomain(request);
        var result = processStockEventUseCase.process(event);
        stockEventMetrics.recordProcessed(result.processedEvent().status(), "rest");
        return stockRestMapper.toResponse(result);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Consulta um evento processado")
    @ApiResponse(responseCode = "200", description = "Evento encontrado")
    @ApiResponse(responseCode = "404", description = "Evento nao encontrado")
    ProcessedEventResponse findProcessedEvent(@PathVariable String eventId) {
        return getProcessedStockEventUseCase.getProcessedEvent(eventId)
                .map(stockRestMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}
