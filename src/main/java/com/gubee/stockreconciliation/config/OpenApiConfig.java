package com.gubee.stockreconciliation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI stockReconciliationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gubee Stock Reconciliation API")
                        .version("v1")
                        .description("API for receiving inventory events and auditing stock reconciliation."));
    }
}
