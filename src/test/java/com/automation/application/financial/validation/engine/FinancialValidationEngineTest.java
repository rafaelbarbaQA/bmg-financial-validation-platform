package com.automation.application.financial.validation.engine;

import com.automation.application.financial.validation.FinancialValidationReport;
import com.automation.application.financial.validation.context.FinancialValidationContext;
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

public final class FinancialValidationEngineTest {

    @Test
    public void shouldPassWhenPaymentIsProcessed() {

        FinancialValidationContext context =
                createContext(
                        "1000.00",
                        "250.00",
                        2
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment()
                        .validate(context);

        assertTrue(
                report.passed()
        );

        assertFalse(
                report.failed()
        );

        assertEquals(
                report.getTotalValidations(),
                1
        );

        assertEquals(
                report.getPassedValidations(),
                1L
        );

        assertEquals(
                report.getFailedValidations(),
                0L
        );

        assertEquals(
                report.getResults()
                        .get(0)
                        .getRule(),
                "PAYMENT_STATUS"
        );

        assertTrue(
                report.getResults()
                        .get(0)
                        .passed()
        );
    }

    @Test
    public void shouldFailWhenPaymentIsNotProcessed() {

        FinancialValidationContext context =
                createContext(
                        "1000.00",
                        "250.00",
                        0
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment()
                        .validate(context);

        assertTrue(
                report.failed()
        );

        assertFalse(
                report.passed()
        );

        assertEquals(
                report.getTotalValidations(),
                1
        );

        assertEquals(
                report.getPassedValidations(),
                0L
        );

        assertEquals(
                report.getFailedValidations(),
                1L
        );

        assertEquals(
                report.getResults()
                        .get(0)
                        .getRule(),
                "PAYMENT_STATUS"
        );

        assertTrue(
                report.getResults()
                        .get(0)
                        .failed()
        );
    }

    @Test
    public void shouldNotAssumeZeroBalanceForFullPayment() {

        FinancialValidationContext context =
                createContext(
                        "1000.00",
                        "375.45",
                        2
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment()
                        .validate(context);

        assertTrue(
                report.passed()
        );

        assertEquals(
                report.getTotalValidations(),
                1
        );

        assertEquals(
                report.getResults()
                        .get(0)
                        .getRule(),
                "PAYMENT_STATUS"
        );
    }

    private FinancialValidationContext createContext(
            String beforeBalance,
            String afterBalance,
            int paymentStatus
    ) {

        return new FinancialValidationContext(
                new FullPaymentScenario(),
                createSnapshot(
                        beforeBalance
                ),
                createSnapshot(
                        afterBalance
                ),
                new BigDecimal(
                        "1000.00"
                ),
                new PaymentProcessingState(
                        paymentStatus
                )
        );
    }

    private FinancialSnapshot createSnapshot(
            String currentBalance
    ) {

        return new FinancialSnapshot(
                new AccountId(
                        "13714323"
                ),
                0,
                "877716729",
                "568338116",
                "9268419047",
                new BigDecimal(
                        currentBalance
                ),
                new BigDecimal(
                        "200.00"
                ),
                new BigDecimal(
                        "1000.00"
                ),
                LocalDate.of(
                        2026,
                        7,
                        5
                ),
                LocalDate.of(
                        2026,
                        7,
                        10
                ),
                LocalDate.of(
                        2026,
                        7,
                        10
                ),
                Instant.parse(
                        "2026-08-09T12:00:00Z"
                )
        );
    }
}