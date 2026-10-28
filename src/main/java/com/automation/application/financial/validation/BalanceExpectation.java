package com.automation.application.financial.validation;

import java.math.BigDecimal;
import java.util.Objects;

public final class BalanceExpectation {

    private final BigDecimal expectedBalance;

    public BalanceExpectation(
            BigDecimal expectedBalance
    ) {
        this.expectedBalance = Objects.requireNonNull(
                expectedBalance,
                "Expected balance cannot be null."
        );
    }

    public BigDecimal getExpectedBalance() {
        return expectedBalance;
    }
}