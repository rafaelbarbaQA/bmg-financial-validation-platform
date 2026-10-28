package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.balance.BalanceValidationService;
import com.automation.application.financial.validation.context.FinancialValidationContext;

import java.util.Objects;

public final class BalanceValidationStep
        implements FinancialValidationStep {

    private final BalanceValidationService balanceValidationService;

    public BalanceValidationStep(
            BalanceValidationService balanceValidationService
    ) {
        this.balanceValidationService = Objects.requireNonNull(
                balanceValidationService,
                "Balance validation service cannot be null."
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

        return balanceValidationService.validate(
                context.getBeforeSnapshot(),
                context.getAfterSnapshot(),
                context.getPaymentAmount()
        );
    }
}
