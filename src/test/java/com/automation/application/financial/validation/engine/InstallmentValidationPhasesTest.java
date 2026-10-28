package com.automation.application.financial.validation.engine;

import com.automation.application.financial.validation.FinancialValidationReport;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.BelowMinimumPaymentScenario;
import com.automation.domain.financial.scenario.FinancialScenario;
import com.automation.domain.financial.scenario.MinimumPaymentScenario;
import com.automation.domain.financial.scenario.Partial20PaymentScenario;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.installment.InstallmentExpectation;
import com.automation.domain.installment.model.InstallmentAdhesionState;
import com.automation.domain.installment.model.InstallmentOfferState;
import com.automation.domain.payment.model.PaymentProcessingState;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class InstallmentValidationPhasesTest {

    @Test
    public void shouldValidateInstallmentOfferOnDueDate() {

        FinancialValidationContext context =
                context(
                        new MinimumPaymentScenario(),
                        new BigDecimal("200.00"),
                        null,
                        new InstallmentOfferState(true),
                        null
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forInstallmentOffer(true)
                        .validate(context);

        assertTrue(report.passed());

        assertEquals(
                report.getTotalValidations(),
                1
        );

        assertEquals(
                report.getResults()
                        .get(0)
                        .getRule(),
                "INSTALLMENT_OFFER"
        );
    }

    @Test
    public void shouldValidateOriginalOfferAdhesionOnFifthBatch() {

        FinancialValidationContext context =
                context(
                        new MinimumPaymentScenario(),
                        new BigDecimal("200.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS),
                        null,
                        new InstallmentAdhesionState(
                                InstallmentExpectation.ORIGINAL_OFFER_ADHESION
                        )
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forInstallmentAdhesion()
                        .validate(context);

        assertTrue(report.passed());

        assertEquals(
                report.getTotalValidations(),
                2
        );

        assertEquals(
                report.getResults()
                        .get(1)
                        .getRule(),
                "INSTALLMENT_ADHESION"
        );
    }

    @Test
    public void shouldValidateNoAdhesionForBelowMinimumPayment() {

        FinancialValidationContext context =
                context(
                        new BelowMinimumPaymentScenario(),
                        new BigDecimal("100.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS),
                        null,
                        new InstallmentAdhesionState(
                                InstallmentExpectation.NO_INSTALLMENT_ADHESION
                        )
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forInstallmentAdhesion()
                        .validate(context);

        assertTrue(report.passed());
    }

    @Test
    public void shouldValidateRecalculatedAdhesionForPartialPayment() {

        FinancialValidationContext context =
                context(
                        new Partial20PaymentScenario(),
                        new BigDecimal("360.00"),
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS),
                        null,
                        new InstallmentAdhesionState(
                                InstallmentExpectation.RECALCULATED_OFFER_ADHESION
                        )
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forInstallmentAdhesion()
                        .validate(context);

        assertTrue(report.passed());
    }

    private FinancialValidationContext context(
            FinancialScenario scenario,
            BigDecimal paymentAmount,
            PaymentProcessingState paymentState,
            InstallmentOfferState offerState,
            InstallmentAdhesionState adhesionState
    ) {

        FinancialSnapshot snapshot =
                snapshot();

        return new FinancialValidationContext(
                scenario,
                snapshot,
                snapshot,
                paymentAmount,
                paymentState,
                offerState,
                adhesionState
        );
    }

    private FinancialSnapshot snapshot() {

        return new FinancialSnapshot(
                new AccountId("13714323"),
                0,
                "877716729",
                "568338116",
                "9268419047",
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 10),
                Instant.parse("2026-08-09T12:00:00Z")
        );
    }
}