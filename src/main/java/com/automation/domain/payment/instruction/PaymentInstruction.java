package com.automation.domain.payment.instruction;

import com.automation.domain.account.AccountId;
import com.automation.domain.payment.PaymentScenario;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class PaymentInstruction {

    private final AccountId accountId;
    private final PaymentScenario scenario;
    private final BigDecimal amount;
    private final Instant preparedAt;

    public PaymentInstruction(
            AccountId accountId,
            PaymentScenario scenario,
            BigDecimal amount,
            Instant preparedAt
    ) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.scenario = Objects.requireNonNull(
                scenario,
                "Payment scenario cannot be null."
        );

        this.amount = requirePositiveAmount(amount);

        this.preparedAt = Objects.requireNonNull(
                preparedAt,
                "Payment preparation date cannot be null."
        );
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public PaymentScenario getScenario() {
        return scenario;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getPreparedAt() {
        return preparedAt;
    }

    private static BigDecimal requirePositiveAmount(
            BigDecimal amount
    ) {
        Objects.requireNonNull(
                amount,
                "Payment amount cannot be null."
        );

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero."
            );
        }

        return amount;
    }
}