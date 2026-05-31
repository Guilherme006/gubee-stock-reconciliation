package com.gubee.stockreconciliation.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class HealthController {

    @GetMapping("/ping")
    PingResponse ping() {
        return new PingResponse("ok");
    }

    record PingResponse(String status) {
    }
}
