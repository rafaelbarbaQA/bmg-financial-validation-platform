package com.automation.application.payment.assertion;

import com.automation.application.payment.validation.PaymentPostBatchValidationResult;
import com.automation.domain.installment.InstallmentExpectation;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentPostBatchAssertions {

    private PaymentPostBatchAssertions() {
        throw new IllegalStateException(
                "PaymentPostBatchAssertions cannot be instantiated."
        );
    }

    public static void assertBelowMinimumPayment(
            PaymentPostBatchValidationResult result
    ) {
        requireResult(result);

        assertBalanceChanged(result);

        assertInstallmentExpectation(
                result,
                InstallmentExpectation.NO_INSTALLMENT_ADHESION
        );
    }

    public static void assertMinimumPayment(
            PaymentPostBatchValidationResult result
    ) {
        requireResult(result);

        assertBalanceChanged(result);

        assertInstallmentExpectation(
                result,
                InstallmentExpectation.ORIGINAL_OFFER_ADHESION
        );
    }

    public static void assertPartialPayment(
            PaymentPostBatchValidationResult result
    ) {
        requireResult(result);

        assertBalanceChanged(result);

        assertInstallmentExpectation(
                result,
                InstallmentExpectation.RECALCULATED_OFFER_ADHESION
        );
    }

    public static void assertFullPayment(
            PaymentPostBatchValidationResult result
    ) {
        requireResult(result);

        /*
         * Full invoice payment does not imply that the current
         * account balance must be zero.
         *
         * Movements performed after invoice closing may remain
         * in the account.
         */
        assertInstallmentExpectation(
                result,
                InstallmentExpectation.NO_INSTALLMENT_ADHESION
        );
    }

    private static void assertBalanceChanged(
            PaymentPostBatchValidationResult result
    ) {
        if (!result.isBalanceChanged()) {
            throw new AssertionError(
                    "Expected account balance to change after payment."
            );
        }

        if (result.getBalanceDifference()
                .compareTo(BigDecimal.ZERO) >= 0) {

            throw new AssertionError(
                    "Expected account balance to decrease after payment. "
                            + "Difference: "
                            + result.getBalanceDifference()
                            + "."
            );
        }
    }

    private static void assertInstallmentExpectation(
            PaymentPostBatchValidationResult result,
            InstallmentExpectation expected
    ) {
        if (result.getInstallmentExpectation() != expected) {
            throw new AssertionError(
                    "Expected installment behavior "
                            + expected
                            + " but was "
                            + result.getInstallmentExpectation()
                            + "."
            );
        }
    }

    private static void requireResult(
            PaymentPostBatchValidationResult result
    ) {
        Objects.requireNonNull(
                result,
                "Payment post-batch validation result cannot be null."
        );
    }
}
