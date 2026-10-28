package com.automation.domain.payment.model;

import com.automation.domain.account.AccountId;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentEventDetails {

    private final long paymentEventId;
    private final AccountId accountId;
    private final BigDecimal amount;
    private final String nossoNumero;
    private final int status;

    public PaymentEventDetails(
            long paymentEventId,
            AccountId accountId,
            BigDecimal amount,
            String nossoNumero,
            int status
    ) {

        if (paymentEventId <= 0) {
            throw new IllegalArgumentException(
                    "Payment event ID must be greater than zero."
            );
        }

        this.paymentEventId = paymentEventId;

        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.amount = Objects.requireNonNull(
                amount,
                "Payment amount cannot be null."
        );

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        this.nossoNumero = requireText(
                nossoNumero,
                "NossoNumero"
        );

        if (status < 0) {
            throw new IllegalArgumentException(
                    "Payment status cannot be negative."
            );
        }

        this.status = status;
    }

    public long getPaymentEventId() {
        return paymentEventId;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNossoNumero() {
        return nossoNumero;
    }

    public int getStatus() {
        return status;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {

        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return normalized;
    }
}