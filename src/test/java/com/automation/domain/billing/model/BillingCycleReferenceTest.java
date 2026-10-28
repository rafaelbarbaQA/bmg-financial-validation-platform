package com.automation.domain.billing.model;

import org.testng.annotations.Test;

import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class BillingCycleReferenceTest {

    @Test
    public void shouldCreateBillingCycleReference() {
        LocalDate previewDate = LocalDate.of(2026, 8, 5);
        LocalDate cutoffDate = LocalDate.of(2026, 8, 10);
        LocalDate dueDate = LocalDate.of(2026, 8, 20);

        BillingCycleReference billingCycle =
                new BillingCycleReference(
                        previewDate,
                        cutoffDate,
                        dueDate
                );

        assertEquals(
                billingCycle.getPreviewDate(),
                previewDate
        );

        assertEquals(
                billingCycle.getExpectedCutoffDate(),
                cutoffDate
        );

        assertEquals(
                billingCycle.getStandardDueDate(),
                dueDate
        );
    }

    @Test
    public void shouldAcceptEqualDates() {
        LocalDate sameDate = LocalDate.of(2026, 8, 10);

        BillingCycleReference billingCycle =
                new BillingCycleReference(
                        sameDate,
                        sameDate,
                        sameDate
                );

        assertEquals(
                billingCycle.getPreviewDate(),
                sameDate
        );

        assertEquals(
                billingCycle.getExpectedCutoffDate(),
                sameDate
        );

        assertEquals(
                billingCycle.getStandardDueDate(),
                sameDate
        );
    }

    @Test
    public void shouldRejectPreviewAfterCutoff() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new BillingCycleReference(
                        LocalDate.of(2026, 8, 15),
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 20)
                )
        );

        assertEquals(
                exception.getMessage(),
                "Preview date cannot be after expected cutoff date."
        );
    }

    @Test
    public void shouldRejectCutoffAfterDueDate() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new BillingCycleReference(
                        LocalDate.of(2026, 8, 5),
                        LocalDate.of(2026, 8, 25),
                        LocalDate.of(2026, 8, 20)
                )
        );

        assertEquals(
                exception.getMessage(),
                "Expected cutoff date cannot be after standard due date."
        );
    }
}