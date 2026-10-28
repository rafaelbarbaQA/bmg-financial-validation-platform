package com.automation.application.financial.validation.engine;

import com.automation.application.financial.validation.FinancialValidationReport;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.step.FinancialValidationStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FinancialValidationEngine
        implements ValidationEngine<FinancialValidationContext> {

    private final List<FinancialValidationStep> steps;

    public FinancialValidationEngine(
            List<FinancialValidationStep> steps
    ) {
        Objects.requireNonNull(
                steps,
                "Validation steps cannot be null."
        );

        if (steps.isEmpty()) {
            throw new IllegalArgumentException(
                    "Financial validation engine requires at least one validation step."
            );
        }

        for (FinancialValidationStep step : steps) {
            Objects.requireNonNull(
                    step,
                    "Validation step cannot be null."
            );
        }

        this.steps = Collections.unmodifiableList(
                new ArrayList<FinancialValidationStep>(steps)
        );
    }

    @Override
    public FinancialValidationReport validate(
            FinancialValidationContext context
    ) {
        Objects.requireNonNull(
                context,
                "Financial validation context cannot be null."
        );

        FinancialValidationReport report =
                new FinancialValidationReport();

        for (FinancialValidationStep step : steps) {

            report.add(
                    step.validate(context)
            );
        }

        return report;
    }

    public int getStepCount() {
        return steps.size();
    }
}
