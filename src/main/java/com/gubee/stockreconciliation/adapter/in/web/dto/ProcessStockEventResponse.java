package com.gubee.stockreconciliation.adapter.in.web.dto;

import java.util.List;

public record ProcessStockEventResponse(
        String eventId,
        String status,
        String detail,
        int currentAvailable,
        List<StockMovementResponse> movements
) {
}
