package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.validator.PaymentProcessingStatusValidator;
import com.automation.domain.payment.model.PaymentProcessingState;

import java.util.Objects;

public final class PaymentProcessingStatusValidationStep
        implements FinancialValidationStep {

    private final PaymentProcessingStatusValidator validator;

    public PaymentProcessingStatusValidationStep(
            PaymentProcessingStatusValidator validator
    ) {
        this.validator = Objects.requireNonNull(
                validator,
                "Payment processing status validator cannot be null."
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

        PaymentProcessingState state =
                context.getPaymentProcessingState()
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment processing state is required for payment status validation."
                                )
                        );

        return validator.validate(
                state.getStatus()
        );
    }
}
