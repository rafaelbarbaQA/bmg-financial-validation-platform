package com.automation.application.financial.validation;

import com.automation.application.financial.runner.PaymentScenarioExecution;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.context.FinancialValidationContextFactory;
import com.automation.application.financial.validation.engine.FinancialValidationEngine;
import com.automation.domain.installment.model.InstallmentAdhesionState;
import com.automation.domain.installment.model.InstallmentOfferState;

import java.util.Objects;

public final class FinancialValidationService {

    private final FinancialValidationContextFactory contextFactory;

    public FinancialValidationService() {
        this(
                new FinancialValidationContextFactory()
        );
    }

    public FinancialValidationService(
            FinancialValidationContextFactory contextFactory
    ) {
        this.contextFactory = Objects.requireNonNull(
                contextFactory,
                "Financial validation context factory cannot be null."
        );
    }

    public FinancialValidationReport validate(
            PaymentScenarioExecution execution,
            FinancialValidationEngine engine
    ) {
        Objects.requireNonNull(
                engine,
                "Financial validation engine cannot be null."
        );

        FinancialValidationContext context =
                contextFactory.create(
                        execution
                );

        return engine.validate(context);
    }

    public FinancialValidationReport validateOffer(
            PaymentScenarioExecution execution,
            InstallmentOfferState offerState,
            FinancialValidationEngine engine
    ) {
        Objects.requireNonNull(
                engine,
                "Financial validation engine cannot be null."
        );

        FinancialValidationContext context =
                contextFactory.createForOffer(
                        execution,
                        offerState
                );

        return engine.validate(context);
    }

    public FinancialValidationReport validateAdhesion(
            PaymentScenarioExecution execution,
            InstallmentAdhesionState adhesionState,
            FinancialValidationEngine engine
    ) {
        Objects.requireNonNull(
                engine,
                "Financial validation engine cannot be null."
        );

        FinancialValidationContext context =
                contextFactory.createForAdhesion(
                        execution,
                        adhesionState
                );

        return engine.validate(context);
    }
}