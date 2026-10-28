package com.automation.application.payment.assertion;

import com.automation.application.payment.validation.PaymentPostBatchValidationResult;
import com.automation.domain.account.AccountId;
import com.automation.domain.installment.InstallmentExpectation;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.expectThrows;

public final class PaymentPostBatchAssertionsTest {

    @Test
    public void shouldAcceptBelowMinimumPayment() {
        PaymentPostBatchAssertions.assertBelowMinimumPayment(
                createResult(
                        "1000.00",
                        "900.00",
                        InstallmentExpectation.NO_INSTALLMENT_ADHESION
                )
        );
    }

    @Test
    public void shouldAcceptMinimumPayment() {
        PaymentPostBatchAssertions.assertMinimumPayment(
                createResult(
                        "1000.00",
                        "800.00",
                        InstallmentExpectation.ORIGINAL_OFFER_ADHESION
                )
        );
    }

    @Test
    public void shouldAcceptPartialPayment() {
        PaymentPostBatchAssertions.assertPartialPayment(
                createResult(
                        "1000.00",
                        "600.00",
                        InstallmentExpectation.RECALCULATED_OFFER_ADHESION
                )
        );
    }

    @Test
    public void shouldAcceptFullPayment() {
        PaymentPostBatchAssertions.assertFullPayment(
                createResult(
                        "1000.00",
                        "0.00",
                        InstallmentExpectation.NO_INSTALLMENT_ADHESION
                )
        );
    }

    @Test
    public void shouldRejectUnchangedBalance() {
        PaymentPostBatchValidationResult result =
                createResult(
                        "1000.00",
                        "1000.00",
                        InstallmentExpectation.NO_INSTALLMENT_ADHESION
                );

        expectThrows(
                AssertionError.class,
                () -> PaymentPostBatchAssertions
                        .assertBelowMinimumPayment(result)
        );
    }

    @Test
    public void shouldRejectBalanceIncrease() {
        PaymentPostBatchValidationResult result =
                createResult(
                        "1000.00",
                        "1100.00",
                        InstallmentExpectation.ORIGINAL_OFFER_ADHESION
                );

        expectThrows(
                AssertionError.class,
                () -> PaymentPostBatchAssertions
                        .assertMinimumPayment(result)
        );
    }

    @Test
    public void shouldRejectIncorrectInstallmentExpectation() {
        PaymentPostBatchValidationResult result =
                createResult(
                        "1000.00",
                        "800.00",
                        InstallmentExpectation.NO_INSTALLMENT_ADHESION
                );

        expectThrows(
                AssertionError.class,
                () -> PaymentPostBatchAssertions
                        .assertMinimumPayment(result)
        );
    }

    @Test
    public void shouldAcceptNonZeroCurrentBalanceAfterFullInvoicePayment() {

        PaymentPostBatchValidationResult result =
                createResult(
                        "1000.00",
                        "50.00",
                        InstallmentExpectation.NO_INSTALLMENT_ADHESION
                );

        PaymentPostBatchAssertions
                .assertFullPayment(result);
    }

    private PaymentPostBatchValidationResult createResult(
            String balanceBefore,
            String balanceAfter,
            InstallmentExpectation installmentExpectation
    ) {
        BigDecimal before =
                new BigDecimal(balanceBefore);

        BigDecimal after =
                new BigDecimal(balanceAfter);

        BigDecimal difference =
                after.subtract(before);

        return new PaymentPostBatchValidationResult(
                new AccountId("13714323"),
                difference.compareTo(BigDecimal.ZERO) != 0,
                before,
                after,
                difference,
                installmentExpectation
        );
    }
}
