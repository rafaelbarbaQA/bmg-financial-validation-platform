package com.automation.application.financial.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FinancialValidationReport {

    private final List<FinancialValidationResult> results =
            new ArrayList<FinancialValidationResult>();

    public FinancialValidationReport add(
            FinancialValidationResult result
    ) {
        results.add(
                Objects.requireNonNull(
                        result,
                        "Financial validation result cannot be null."
                )
        );

        return this;
    }

    public List<FinancialValidationResult> getResults() {
        return Collections.unmodifiableList(
                results
        );
    }

    public boolean passed() {
        return !results.isEmpty()
                && results.stream()
                .allMatch(FinancialValidationResult::passed);
    }

    public boolean failed() {
        return results.stream()
                .anyMatch(FinancialValidationResult::failed);
    }

    public int getTotalValidations() {
        return results.size();
    }

    public long getPassedValidations() {
        return results.stream()
                .filter(FinancialValidationResult::passed)
                .count();
    }

    public long getFailedValidations() {
        return results.stream()
                .filter(FinancialValidationResult::failed)
                .count();
    }
}
