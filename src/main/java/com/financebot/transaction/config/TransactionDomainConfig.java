package com.financebot.transaction.config;

import com.financebot.transaction.domain.installment.InstallmentPlanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionDomainConfig {

    @Bean
    public InstallmentPlanFactory installmentPlanFactory() {
        return new InstallmentPlanFactory();
    }
}