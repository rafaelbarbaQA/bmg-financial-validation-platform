package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.validator.AccountStatusValidator;

import java.util.Objects;

public final class AccountStatusValidationStep
        implements FinancialValidationStep {

    private final int expectedStatus;
    private final AccountStatusValidator validator;

    public AccountStatusValidationStep(
            int expectedStatus,
            AccountStatusValidator validator
    ) {
        this.expectedStatus = expectedStatus;

        this.validator = Objects.requireNonNull(
                validator,
                "Account status validator cannot be null."
        );
    }

    @Override
    public FinancialValidationResult validate(
            FinancialValidationContext context
    ) {
        Objects.requireNonNull(
                context,
                "Financial validation context cannot be null."
        );

        return validator.validate(
                expectedStatus,
                context.getAfterSnapshot()
                        .getAccountStatus()
        );
    }
}
