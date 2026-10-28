package com.automation.application.financial.validation;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public final class FinancialValidationReportTest {

    @Test
    public void shouldPassWhenAllValidationsPass() {

        FinancialValidationReport report =
                new FinancialValidationReport()
                        .add(pass("BALANCE"))
                        .add(pass("ACCOUNT_STATUS"))
                        .add(pass("INSTALLMENT"));

        assertTrue(report.passed());
        assertFalse(report.failed());

        assertEquals(
                report.getTotalValidations(),
                3
        );

        assertEquals(
                report.getPassedValidations(),
                3L
        );

        assertEquals(
                report.getFailedValidations(),
                0L
        );
    }

    @Test
    public void shouldFailWhenAtLeastOneValidationFails() {

        FinancialValidationReport report =
                new FinancialValidationReport()
                        .add(pass("BALANCE"))
                        .add(fail("PAYMENT_STATUS"))
                        .add(pass("INSTALLMENT"));

        assertFalse(report.passed());
        assertTrue(report.failed());

        assertEquals(
                report.getTotalValidations(),
                3
        );

        assertEquals(
                report.getPassedValidations(),
                2L
        );

        assertEquals(
                report.getFailedValidations(),
                1L
        );
    }

    @Test
    public void shouldNotPassWhenReportHasNoValidations() {

        FinancialValidationReport report =
                new FinancialValidationReport();

        assertFalse(report.passed());
        assertFalse(report.failed());

        assertEquals(
                report.getTotalValidations(),
                0
        );
    }

    private FinancialValidationResult pass(
            String rule
    ) {
        return new FinancialValidationResult(
                rule,
                FinancialValidationStatus.PASS,
                "EXPECTED",
                "EXPECTED",
                "Validation passed."
        );
    }

    private FinancialValidationResult fail(
            String rule
    ) {
        return new FinancialValidationResult(
                rule,
                FinancialValidationStatus.FAIL,
                "EXPECTED",
                "ACTUAL",
                "Validation failed."
        );
    }
}
