package com.automation.application.financial.validation.validator;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.FinancialValidationStatus;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.math.BigDecimal;
import java.util.Objects;

public final class BalanceValidator {

    public FinancialValidationResult validate(
            FinancialSnapshot after,
            BigDecimal expectedBalance
    ) {

        Objects.requireNonNull(
                after,
                "After snapshot cannot be null."
        );

        Objects.requireNonNull(
                expectedBalance,
                "Expected balance cannot be null."
        );

        BigDecimal actualBalance =
                after.getCurrentBalance();

        boolean matches =
                expectedBalance.compareTo(
                        actualBalance
                ) == 0;

        if (matches) {

            return new FinancialValidationResult(
                    "BALANCE",
                    FinancialValidationStatus.PASS,
                    expectedBalance.toPlainString(),
                    actualBalance.toPlainString(),
                    "Account balance matches expected value."
            );
        }

        return new FinancialValidationResult(
                "BALANCE",
                FinancialValidationStatus.FAIL,
                expectedBalance.toPlainString(),
                actualBalance.toPlainString(),
                "Account balance differs from expected value."
        );
    }
}
