package com.automation.application.financial.validation;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class FinancialValidationResultTest {

    @Test
    public void shouldCreatePassedValidation() {

        FinancialValidationResult result =
                new FinancialValidationResult(
                        "Balance",
                        FinancialValidationStatus.PASS,
                        "0.00",
                        "0.00",
                        "Balance validated."
                );

        assertTrue(result.passed());

        assertFalse(result.failed());

        assertEquals(
                result.getRule(),
                "Balance"
        );

    }

}
