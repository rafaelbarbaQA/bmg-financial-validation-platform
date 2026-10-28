package com.automation.application.financial.validation;

import java.util.Objects;

public final class FinancialValidationResult {

    private final String rule;

    private final FinancialValidationStatus status;

    private final String expected;

    private final String actual;

    private final String message;

    public FinancialValidationResult(
            String rule,
            FinancialValidationStatus status,
            String expected,
            String actual,
            String message
    ) {

        this.rule = Objects.requireNonNull(rule);

        this.status = Objects.requireNonNull(status);

        this.expected = Objects.requireNonNull(expected);

        this.actual = Objects.requireNonNull(actual);

        this.message = Objects.requireNonNull(message);

    }

    public String getRule() {
        return rule;
    }

    public FinancialValidationStatus getStatus() {
        return status;
    }

    public String getExpected() {
        return expected;
    }

    public String getActual() {
        return actual;
    }

    public String getMessage() {
        return message;
    }

    public boolean passed() {
        return status == FinancialValidationStatus.PASS;
    }

    public boolean failed() {
        return status == FinancialValidationStatus.FAIL;
    }

}
