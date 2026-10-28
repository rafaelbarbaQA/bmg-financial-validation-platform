package com.automation.domain.installment;

import java.math.BigDecimal;
import java.util.Objects;

public final class InstallmentExpectationResolver {

    public InstallmentExpectation resolve(
            BigDecimal paymentAmount,
            BigDecimal minimumAmount,
            BigDecimal invoiceAmount
    ) {
        Objects.requireNonNull(
                paymentAmount,
                "Payment amount cannot be null."
        );

        Objects.requireNonNull(
                minimumAmount,
                "Minimum amount cannot be null."
        );

        Objects.requireNonNull(
                invoiceAmount,
                "Invoice amount cannot be null."
        );

        validateAmounts(
                paymentAmount,
                minimumAmount,
                invoiceAmount
        );

        if (paymentAmount.compareTo(minimumAmount) < 0) {
            return InstallmentExpectation
                    .NO_INSTALLMENT_ADHESION;
        }

        if (paymentAmount.compareTo(minimumAmount) == 0) {
            return InstallmentExpectation
                    .ORIGINAL_OFFER_ADHESION;
        }

        if (paymentAmount.compareTo(invoiceAmount) < 0) {
            return InstallmentExpectation
                    .RECALCULATED_OFFER_ADHESION;
        }

        if (paymentAmount.compareTo(invoiceAmount) == 0) {
            return InstallmentExpectation
                    .NO_INSTALLMENT_ADHESION;
        }

        throw new IllegalArgumentException(
                "No installment expectation defined for payment amount "
                        + paymentAmount
                        + " above invoice amount "
                        + invoiceAmount
                        + "."
        );
    }

    private static void validateAmounts(
            BigDecimal paymentAmount,
            BigDecimal minimumAmount,
            BigDecimal invoiceAmount
    ) {
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        if (minimumAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Minimum amount cannot be negative."
            );
        }

        if (invoiceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Invoice amount must be greater than zero."
            );
        }

        if (minimumAmount.compareTo(invoiceAmount) > 0) {
            throw new IllegalArgumentException(
                    "Minimum amount cannot be greater than invoice amount."
            );
        }
    }
}