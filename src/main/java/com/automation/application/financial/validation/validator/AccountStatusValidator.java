package com.automation.application.financial.validation.validator;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.FinancialValidationStatus;

public final class AccountStatusValidator {

    public FinancialValidationResult validate(
            int expectedStatus,
            int actualStatus
    ) {
        boolean matches =
                expectedStatus == actualStatus;

        if (matches) {
            return new FinancialValidationResult(
                    "ACCOUNT_STATUS",
                    FinancialValidationStatus.PASS,
                    String.valueOf(expectedStatus),
                    String.valueOf(actualStatus),
                    "Account status matches expected value."
            );
        }

        return new FinancialValidationResult(
                "ACCOUNT_STATUS",
                FinancialValidationStatus.FAIL,
                String.valueOf(expectedStatus),
                String.valueOf(actualStatus),
                "Account status differs from expected value."
        );
    }
}
