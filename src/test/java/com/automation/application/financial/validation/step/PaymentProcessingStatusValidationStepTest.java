package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.validator.PaymentProcessingStatusValidator;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.FullPaymentScenario;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.model.PaymentProcessingState;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public final class PaymentProcessingStatusValidationStepTest {

    private final PaymentProcessingStatusValidationStep step =
            new PaymentProcessingStatusValidationStep(
                    new PaymentProcessingStatusValidator()
            );

    @Test
    public void shouldPassWhenPaymentStatusIsProcessed() {

        FinancialValidationContext context =
                createContext(2);

        FinancialValidationResult result =
                step.validate(context);

        assertTrue(result.passed());
        assertFalse(result.failed());

        assertEquals(
                result.getRule(),
                "PAYMENT_STATUS"
        );

        assertEquals(
                result.getExpected(),
                "2"
        );

        assertEquals(
                result.getActual(),
                "2"
        );
    }

    @Test
    public void shouldFailWhenPaymentStatusIsNotProcessed() {

        FinancialValidationContext context =
                createContext(0);

        FinancialValidationResult result =
                step.validate(context);

        assertTrue(result.failed());
        assertFalse(result.passed());

        assertEquals(
                result.getExpected(),
                "2"
        );

        assertEquals(
                result.getActual(),
                "0"
        );
    }

    private FinancialValidationContext createContext(
            int paymentStatus
    ) {
        FinancialSnapshot before =
                createSnapshot("1000.00");

        FinancialSnapshot after =
                createSnapshot("0.00");

        return new FinancialValidationContext(
                new FullPaymentScenario(),
                before,
                after,
                new BigDecimal("1000.00"),
                new PaymentProcessingState(
                        paymentStatus
                )
        );
    }

    private FinancialSnapshot createSnapshot(
            String currentBalance
    ) {
        return new FinancialSnapshot(
                new AccountId("13714323"),
                0,
                "877716729",
                "568338116",
                "9268419047",
                new BigDecimal(currentBalance),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 10),
                Instant.parse("2026-08-09T12:00:00Z")
        );
    }
}
