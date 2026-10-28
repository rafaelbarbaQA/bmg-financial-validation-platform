package com.automation.application.payment.validation;

import com.automation.domain.account.AccountId;
import com.automation.domain.installment.InstallmentExpectation;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentPostBatchValidationResult {

    private final AccountId accountId;
    private final boolean balanceChanged;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final BigDecimal balanceDifference;
    private final InstallmentExpectation installmentExpectation;

    public PaymentPostBatchValidationResult(
            AccountId accountId,
            boolean balanceChanged,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            BigDecimal balanceDifference,
            InstallmentExpectation installmentExpectation
    ) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.balanceChanged = balanceChanged;

        this.balanceBefore = Objects.requireNonNull(
                balanceBefore,
                "Balance before cannot be null."
        );

        this.balanceAfter = Objects.requireNonNull(
                balanceAfter,
                "Balance after cannot be null."
        );

        this.balanceDifference = Objects.requireNonNull(
                balanceDifference,
                "Balance difference cannot be null."
        );

        this.installmentExpectation = Objects.requireNonNull(
                installmentExpectation,
                "Installment expectation cannot be null."
        );
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public boolean isBalanceChanged() {
        return balanceChanged;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getBalanceDifference() {
        return balanceDifference;
    }

    public InstallmentExpectation getInstallmentExpectation() {
        return installmentExpectation;
    }
}