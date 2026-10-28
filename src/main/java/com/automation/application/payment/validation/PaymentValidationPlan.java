package com.automation.application.payment.validation;

import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.installment.InstallmentExpectation;
import com.automation.domain.payment.PaymentScenario;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentValidationPlan {

    private final FinancialSnapshot beforeSnapshot;
    private final PaymentScenario scenario;
    private final BigDecimal paymentAmount;
    private final InstallmentExpectation installmentExpectation;

    public PaymentValidationPlan(
            FinancialSnapshot beforeSnapshot,
            PaymentScenario scenario,
            BigDecimal paymentAmount,
            InstallmentExpectation installmentExpectation
    ) {
        this.beforeSnapshot = Objects.requireNonNull(
                beforeSnapshot,
                "Before snapshot cannot be null."
        );

        this.scenario = Objects.requireNonNull(
                scenario,
                "Payment scenario cannot be null."
        );

        this.paymentAmount = requirePositive(
                paymentAmount,
                "Payment amount"
        );

        this.installmentExpectation = Objects.requireNonNull(
                installmentExpectation,
                "Installment expectation cannot be null."
        );
    }

    public FinancialSnapshot getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public PaymentScenario getScenario() {
        return scenario;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public InstallmentExpectation getInstallmentExpectation() {
        return installmentExpectation;
    }

    private static BigDecimal requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }

        return value;
    }
}