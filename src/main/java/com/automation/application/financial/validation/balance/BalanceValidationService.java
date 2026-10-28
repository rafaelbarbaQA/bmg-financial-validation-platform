package com.automation.application.financial.validation.balance;

import com.automation.application.financial.validation.BalanceExpectation;
import com.automation.application.financial.validation.BalanceExpectationCalculator;
import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.validator.BalanceValidator;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.math.BigDecimal;
import java.util.Objects;

public final class BalanceValidationService {

    private final BalanceExpectationCalculator expectationCalculator;
    private final BalanceValidator balanceValidator;

    public BalanceValidationService(
            BalanceExpectationCalculator expectationCalculator,
            BalanceValidator balanceValidator
    ) {
        this.expectationCalculator = Objects.requireNonNull(
                expectationCalculator,
                "Balance expectation calculator cannot be null."
        );

        this.balanceValidator = Objects.requireNonNull(
                balanceValidator,
                "Balance validator cannot be null."
        );
    }

    public FinancialValidationResult validate(
            FinancialSnapshot before,
            FinancialSnapshot after,
            BigDecimal paymentAmount
    ) {
        Objects.requireNonNull(
                before,
                "Before snapshot cannot be null."
        );

        Objects.requireNonNull(
                after,
                "After snapshot cannot be null."
        );

        Objects.requireNonNull(
                paymentAmount,
                "Payment amount cannot be null."
        );

        validateSameAccount(
                before,
                after
        );

        BalanceExpectation expectation =
                expectationCalculator.calculate(
                        before,
                        paymentAmount
                );

        return balanceValidator.validate(
                after,
                expectation.getExpectedBalance()
        );
    }

    private static void validateSameAccount(
            FinancialSnapshot before,
            FinancialSnapshot after
    ) {
        if (!before.getAccountId()
                .equals(after.getAccountId())) {

            throw new IllegalArgumentException(
                    "Before and after snapshots must belong to the same account."
            );
        }
    }
}
