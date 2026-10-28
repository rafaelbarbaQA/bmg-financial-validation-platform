package com.automation.domain.billing.model;

import java.time.LocalDate;
import java.util.Objects;

public final class BillingCycleReference {

    private final LocalDate previewDate;
    private final LocalDate expectedCutoffDate;
    private final LocalDate standardDueDate;

    public BillingCycleReference(
            LocalDate previewDate,
            LocalDate expectedCutoffDate,
            LocalDate standardDueDate
    ) {
        this.previewDate = Objects.requireNonNull(
                previewDate,
                "Preview date cannot be null."
        );

        this.expectedCutoffDate = Objects.requireNonNull(
                expectedCutoffDate,
                "Expected cutoff date cannot be null."
        );

        this.standardDueDate = Objects.requireNonNull(
                standardDueDate,
                "Standard due date cannot be null."
        );

        validateDateOrder();
    }

    public LocalDate getPreviewDate() {
        return previewDate;
    }

    public LocalDate getExpectedCutoffDate() {
        return expectedCutoffDate;
    }

    public LocalDate getStandardDueDate() {
        return standardDueDate;
    }

    private void validateDateOrder() {
        if (previewDate.isAfter(expectedCutoffDate)) {
            throw new IllegalArgumentException(
                    "Preview date cannot be after expected cutoff date."
            );
        }

        if (expectedCutoffDate.isAfter(standardDueDate)) {
            throw new IllegalArgumentException(
                    "Expected cutoff date cannot be after standard due date."
            );
        }
    }
}