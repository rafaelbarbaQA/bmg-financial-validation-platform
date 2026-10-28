package com.automation.application.payment.validation;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.installment.InstallmentExpectation;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;

public final class PaymentValidationPlanFactoryTest {

    private static final AccountId ACCOUNT_ID =
            new AccountId("13714323");

    private static final Instant CAPTURED_AT =
            Instant.parse("2026-08-11T20:00:00Z");

    private PaymentValidationPlanFactory factory;

    private FinancialSnapshot snapshot;

    @BeforeMethod
    public void setUp() {

        factory =
                new PaymentValidationPlanFactory();

        /*
         * Current account balance is intentionally greater
         * than invoice amount.
         */
        snapshot =
                new FinancialSnapshot(
                        ACCOUNT_ID,
                        0,
                        "877716729",
                        "568338116",
                        "9268419047",
                        new BigDecimal("1200.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 7, 10),
                        LocalDate.of(2026, 7, 10),
                        CAPTURED_AT
                );
    }

    @Test
    public void shouldResolveFullInvoicePaymentUsingInvoiceAmountNotCurrentBalance() {

        PaymentValidationPlan plan =
                createPlan(
                        PaymentScenario.TOTAL,
                        "1000.00"
                );

        assertEquals(
                plan.getInstallmentExpectation(),
                InstallmentExpectation
                        .NO_INSTALLMENT_ADHESION
        );
    }

    @Test
    public void shouldResolvePartialPaymentUsingInvoiceAmount() {

        PaymentValidationPlan plan =
                createPlan(
                        PaymentScenario
                                .BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT,
                        "680.00"
                );

        assertEquals(
                plan.getInstallmentExpectation(),
                InstallmentExpectation
                        .RECALCULATED_OFFER_ADHESION
        );
    }

    @Test
    public void shouldResolveMinimumPayment() {

        PaymentValidationPlan plan =
                createPlan(
                        PaymentScenario.MINIMUM,
                        "200.00"
                );

        assertEquals(
                plan.getInstallmentExpectation(),
                InstallmentExpectation
                        .ORIGINAL_OFFER_ADHESION
        );
    }

    @Test
    public void shouldResolveBelowMinimumPayment() {

        PaymentValidationPlan plan =
                createPlan(
                        PaymentScenario.BELOW_MINIMUM,
                        "100.00"
                );

        assertEquals(
                plan.getInstallmentExpectation(),
                InstallmentExpectation
                        .NO_INSTALLMENT_ADHESION
        );
    }

    private PaymentValidationPlan createPlan(
            PaymentScenario scenario,
            String amount
    ) {

        PaymentInstruction instruction =
                new PaymentInstruction(
                        ACCOUNT_ID,
                        scenario,
                        new BigDecimal(amount),
                        CAPTURED_AT
                );

        return factory.create(
                snapshot,
                instruction
        );
    }
}