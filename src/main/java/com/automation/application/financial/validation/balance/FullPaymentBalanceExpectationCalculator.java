package com.automation.application.financial.validation.balance;

import com.automation.application.financial.validation.BalanceExpectation;
import com.automation.application.financial.validation.BalanceExpectationCalculator;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.math.BigDecimal;
import java.util.Objects;

public final class FullPaymentBalanceExpectationCalculator
        implements BalanceExpectationCalculator {

    @Override
    public BalanceExpectation calculate(
            FinancialSnapshot before,
            BigDecimal paymentAmount
    ) {
        Objects.requireNonNull(
                before,
                "Before snapshot cannot be null."
        );

        Objects.requireNonNull(
                paymentAmount,
                "Payment amount cannot be null."
        );

        if (paymentAmount.compareTo(
                before.getInvoiceAmount()
        ) != 0) {

            throw new IllegalArgumentException(
                    "Full payment amount must match invoice amount."
            );
        }

        BigDecimal expectedBalance =
                before.getCurrentBalance()
                        .subtract(paymentAmount);

        return new BalanceExpectation(
                expectedBalance
        );
    }
}