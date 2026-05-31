package com.gubee.stockreconciliation.config;

import com.gubee.stockreconciliation.application.service.StockReconciliationApplicationService;
import com.gubee.stockreconciliation.domain.policy.StockReconciler;
import com.gubee.stockreconciliation.domain.port.out.StockLedgerPort;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationLockPort;
import com.gubee.stockreconciliation.domain.port.out.StockReconciliationStatePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationUseCaseConfig {

    @Bean
    StockReconciler stockReconciler() {
        return new StockReconciler();
    }

    @Bean
    @ConditionalOnBean({
            StockLedgerPort.class,
            StockReconciliationStatePort.class,
            StockReconciliationLockPort.class
    })
    StockReconciliationApplicationService stockReconciliationApplicationService(
            StockLedgerPort stockLedgerPort,
            StockReconciliationStatePort stockReconciliationStatePort,
            StockReconciliationLockPort stockReconciliationLockPort,
            StockReconciler stockReconciler
    ) {
        return new StockReconciliationApplicationService(
                stockLedgerPort,
                stockReconciliationStatePort,
                stockReconciliationLockPort,
                stockReconciler
        );
    }
}
