package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.validator.InstallmentAdhesionValidator;
import com.automation.domain.installment.model.InstallmentAdhesionState;

import java.util.Objects;

public final class InstallmentAdhesionValidationStep
        implements FinancialValidationStep {

    private final InstallmentAdhesionValidator validator;

    public InstallmentAdhesionValidationStep(
            InstallmentAdhesionValidator validator
    ) {
        this.validator = Objects.requireNonNull(
                validator,
                "Installment adhesion validator cannot be null."
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

        InstallmentAdhesionState state =
                context.getInstallmentAdhesionState()
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Installment adhesion state is required."
                                )
                        );

        return validator.validate(
                context.getScenario()
                        .expectedInstallment(),
                state.getObservedOutcome()
        );
    }
}