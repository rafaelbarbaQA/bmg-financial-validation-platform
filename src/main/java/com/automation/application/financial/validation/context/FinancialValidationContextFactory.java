package com.automation.application.financial.validation.context;

import com.automation.application.financial.runner.PaymentScenarioExecution;
import com.automation.domain.installment.model.InstallmentAdhesionState;
import com.automation.domain.installment.model.InstallmentOfferState;

import java.util.Objects;

public final class FinancialValidationContextFactory {

    public FinancialValidationContext create(
            PaymentScenarioExecution execution
    ) {
        validateCommonState(execution);
        requirePaymentProcessingState(execution);

        return new FinancialValidationContext(
                execution.getScenario(),
                execution.getBeforeSnapshot(),
                execution.getAfterSnapshot(),
                execution.getPaymentInstruction().getAmount(),
                execution.getPaymentProcessingState()
        );
    }

    public FinancialValidationContext createForOffer(
            PaymentScenarioExecution execution,
            InstallmentOfferState offerState
    ) {
        validateCommonState(execution);

        Objects.requireNonNull(
                offerState,
                "Installment offer state cannot be null."
        );

        return new FinancialValidationContext(
                execution.getScenario(),
                execution.getBeforeSnapshot(),
                execution.getAfterSnapshot(),
                execution.getPaymentInstruction().getAmount(),
                execution.getPaymentProcessingState(),
                offerState,
                null
        );
    }

    public FinancialValidationContext createForAdhesion(
            PaymentScenarioExecution execution,
            InstallmentAdhesionState adhesionState
    ) {
        validateCommonState(execution);
        requirePaymentProcessingState(execution);

        Objects.requireNonNull(
                adhesionState,
                "Installment adhesion state cannot be null."
        );

        return new FinancialValidationContext(
                execution.getScenario(),
                execution.getBeforeSnapshot(),
                execution.getAfterSnapshot(),
                execution.getPaymentInstruction().getAmount(),
                execution.getPaymentProcessingState(),
                null,
                adhesionState
        );
    }

    private static void validateCommonState(
            PaymentScenarioExecution execution
    ) {
        Objects.requireNonNull(
                execution,
                "Payment scenario execution cannot be null."
        );

        if (execution.getBeforeSnapshot() == null) {
            throw new IllegalStateException(
                    "Before snapshot is required for financial validation."
            );
        }

        if (execution.getAfterSnapshot() == null) {
            throw new IllegalStateException(
                    "After snapshot is required for financial validation."
            );
        }

        if (execution.getPaymentInstruction() == null) {
            throw new IllegalStateException(
                    "Payment instruction is required for financial validation."
            );
        }
    }

    private static void requirePaymentProcessingState(
            PaymentScenarioExecution execution
    ) {
        if (execution.getPaymentProcessingState() == null) {
            throw new IllegalStateException(
                    "Payment processing state is required for this validation phase."
            );
        }
    }
}