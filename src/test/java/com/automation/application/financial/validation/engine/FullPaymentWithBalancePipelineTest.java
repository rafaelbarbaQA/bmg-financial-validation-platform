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
import static org.testng.Assert.assertTrue;

public final class FullPaymentWithBalancePipelineTest {

    @Test
    public void shouldPassWhenPaymentIsProcessedAndBalanceIsCleared() {

        FinancialSnapshot before =
                createSnapshot(
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "0.00"
                );

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        before,
                        after,
                        new BigDecimal("1000.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPaymentWithBalance()
                        .validate(context);

        assertEquals(
                report.getTotalValidations(),
                2
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
                "BALANCE"
        );

        assertTrue(
                report.passed()
        );
    }

    @Test
    public void shouldPreservePostClosingMovementsAfterFullInvoicePayment() {

        FinancialSnapshot before =
                createSnapshot(
                        "1200.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "200.00"
                );

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        before,
                        after,
                        new BigDecimal("1000.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPaymentWithBalance()
                        .validate(context);

        assertEquals(
                report.getTotalValidations(),
                2
        );

        assertTrue(
                report.getResults()
                        .get(0)
                        .passed()
        );

        assertTrue(
                report.getResults()
                        .get(1)
                        .passed()
        );

        assertEquals(
                report.getResults()
                        .get(1)
                        .getExpected(),
                "200.00"
        );

        assertEquals(
                report.getResults()
                        .get(1)
                        .getActual(),
                "200.00"
        );

        assertTrue(
                report.passed()
        );
    }

    @Test
    public void shouldFailWhenBalanceIsNotCleared() {

        FinancialSnapshot before =
                createSnapshot(
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "200.00"
                );

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        before,
                        after,
                        new BigDecimal("1000.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPaymentWithBalance()
                        .validate(context);

        assertEquals(
                report.getTotalValidations(),
                2
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

        assertTrue(
                report.failed()
        );
    }
    @Test
    public void shouldFailWhenAccountStatusDiffers() {

        FinancialSnapshot before =
                createSnapshot(
                        "1000.00",
                        0
                );

        FinancialSnapshot after =
                createSnapshot(
                        "0.00",
                        10
                );

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        before,
                        after,
                        new BigDecimal("1000.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPaymentWithBalance(0)
                        .validate(context);

        assertEquals(
                report.getTotalValidations(),
                3
        );

        assertEquals(
                report.getResults()
                        .get(2)
                        .getRule(),
                "ACCOUNT_STATUS"
        );

        assertTrue(
                report.getResults()
                        .get(0)
                        .passed()
        );

        assertTrue(
                report.getResults()
                        .get(1)
                        .passed()
        );

        assertTrue(
                report.getResults()
                        .get(2)
                        .failed()
        );

        assertTrue(
                report.failed()
        );
    }
    @Test
    public void shouldFailWhenPaymentIsNotProcessed() {

        FinancialSnapshot before =
                createSnapshot(
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "0.00"
                );

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        before,
                        after,
                        new BigDecimal("1000.00"),
                        new PaymentProcessingState(PaymentProcessingState.WAITING_FOR_BATCH_STATUS)
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPaymentWithBalance()
                        .validate(context);

        assertEquals(
                report.getTotalValidations(),
                2
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

        assertTrue(
                report.getResults()
                        .get(1)
                        .passed()
        );

        assertTrue(
                report.failed()
        );
    }
    private static FinancialSnapshot createSnapshot(
            String balance
    ) {
        return createSnapshot(
                balance,
                0
        );
    }

    private static FinancialSnapshot createSnapshot(
            String balance,
            int accountStatus
    ) {

        return new FinancialSnapshot(
                new AccountId("13714323"),
                accountStatus,
                "HISTORY",
                "TICKET",
                "NOSSO-NUMERO",
                new BigDecimal(balance),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 10),
                Instant.parse(
                        "2026-08-16T12:00:00Z"
                )
        );
    }
}