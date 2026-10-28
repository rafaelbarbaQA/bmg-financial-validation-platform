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

public final class FinancialValidationPipelineTest {

    @Test
    public void shouldPassFullPaymentValidationWithoutAssumingZeroBalance() {

        FinancialValidationContext context =
                createContext(
                        "1000.00",
                        "250.00",
                        2,
                        0
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment(0)
                        .validate(context);

        assertTrue(
                report.passed()
        );

        assertFalse(
                report.failed()
        );

        assertEquals(
                report.getTotalValidations(),
                2
        );

        assertEquals(
                report.getPassedValidations(),
                2L
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

        assertEquals(
                report.getResults()
                        .get(1)
                        .getRule(),
                "ACCOUNT_STATUS"
        );
    }

    @Test
    public void shouldFailOnlyAccountStatusValidation() {

        FinancialValidationContext context =
                createContext(
                        "1000.00",
                        "250.00",
                        2,
                        10
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment(0)
                        .validate(context);

        assertTrue(
                report.failed()
        );

        assertEquals(
                report.getPassedValidations(),
                1L
        );

        assertEquals(
                report.getFailedValidations(),
                1L
        );

        assertTrue(
                report.getResults()
                        .get(0)
                        .passed()
        );

        assertTrue(
                report.getResults()
                        .get(1)
                        .failed()
        );

        assertEquals(
                report.getResults()
                        .get(1)
                        .getExpected(),
                "0"
        );

        assertEquals(
                report.getResults()
                        .get(1)
                        .getActual(),
                "10"
        );
    }

    @Test
    public void shouldConsolidatePaymentAndAccountFailures() {

        FinancialValidationContext context =
                createContext(
                        "1000.00",
                        "75.30",
                        0,
                        10
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment(0)
                        .validate(context);

        assertTrue(
                report.failed()
        );

        assertEquals(
                report.getTotalValidations(),
                2
        );

        assertEquals(
                report.getPassedValidations(),
                0L
        );

        assertEquals(
                report.getFailedValidations(),
                2L
        );
    }

    private FinancialValidationContext createContext(
            String beforeBalance,
            String afterBalance,
            int paymentStatus,
            int accountStatus
    ) {

        return new FinancialValidationContext(
                new FullPaymentScenario(),
                createSnapshot(
                        beforeBalance,
                        0
                ),
                createSnapshot(
                        afterBalance,
                        accountStatus
                ),
                new BigDecimal("1000.00"),
                new PaymentProcessingState(
                        paymentStatus
                )
        );
    }

    private FinancialSnapshot createSnapshot(
            String currentBalance,
            int accountStatus
    ) {

        return new FinancialSnapshot(
                new AccountId("13714323"),
                accountStatus,
                "877716729",
                "568338116",
                "9268419047",
                new BigDecimal(
                        currentBalance
                ),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
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