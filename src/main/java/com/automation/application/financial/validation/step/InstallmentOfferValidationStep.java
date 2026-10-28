package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.validator.InstallmentOfferValidator;
import com.automation.domain.installment.model.InstallmentOfferState;

import java.util.Objects;

public final class InstallmentOfferValidationStep
        implements FinancialValidationStep {

    private final boolean expectedOffer;
    private final InstallmentOfferValidator validator;

    public InstallmentOfferValidationStep(
            boolean expectedOffer,
            InstallmentOfferValidator validator
    ) {
        this.expectedOffer = expectedOffer;

        this.validator = Objects.requireNonNull(
                validator,
                "Installment offer validator cannot be null."
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

        InstallmentOfferState state =
                context.getInstallmentOfferState()
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Installment offer state is required."
                                )
                        );

        return validator.validate(
                expectedOffer,
                state.isOfferAvailable()
        );
    }
}