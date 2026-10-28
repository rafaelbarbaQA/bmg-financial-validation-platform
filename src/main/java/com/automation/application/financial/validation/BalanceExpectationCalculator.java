package com.automation.application.financial.validation;

import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.math.BigDecimal;

public interface BalanceExpectationCalculator {

    BalanceExpectation calculate(
            FinancialSnapshot before,
            BigDecimal paymentAmount
    );
}
