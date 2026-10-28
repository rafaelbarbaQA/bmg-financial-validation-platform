package com.automation.application.financial.validation.context;

import com.automation.domain.financial.scenario.FinancialScenario;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.installment.model.InstallmentAdhesionState;
import com.automation.domain.installment.model.InstallmentOfferState;
import com.automation.domain.payment.model.PaymentProcessingState;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public final class FinancialValidationContext {

    private final FinancialScenario scenario;
    private final FinancialSnapshot beforeSnapshot;
    private final FinancialSnapshot afterSnapshot;
    private final BigDecimal paymentAmount;
    private final PaymentProcessingState paymentProcessingState;
    private final InstallmentOfferState installmentOfferState;
    private final InstallmentAdhesionState installmentAdhesionState;

    public FinancialValidationContext(
            FinancialScenario scenario,
            FinancialSnapshot beforeSnapshot,
            FinancialSnapshot afterSnapshot,
            BigDecimal paymentAmount
    ) {
        this(
                scenario,
                beforeSnapshot,
                afterSnapshot,
                paymentAmount,
                null,
                null,
                null
        );
    }

    public FinancialValidationContext(
            FinancialScenario scenario,
            FinancialSnapshot beforeSnapshot,
            FinancialSnapshot afterSnapshot,
            BigDecimal paymentAmount,
            PaymentProcessingState paymentProcessingState
    ) {
        this(
                scenario,
                beforeSnapshot,
                afterSnapshot,
                paymentAmount,
                paymentProcessingState,
                null,
                null
        );
    }

    public FinancialValidationContext(
            FinancialScenario scenario,
            FinancialSnapshot beforeSnapshot,
            FinancialSnapshot afterSnapshot,
            BigDecimal paymentAmount,
            PaymentProcessingState paymentProcessingState,
            InstallmentOfferState installmentOfferState,
            InstallmentAdhesionState installmentAdhesionState
    ) {
        this.scenario = Objects.requireNonNull(
                scenario,
                "Financial scenario cannot be null."
        );

        this.beforeSnapshot = Objects.requireNonNull(
                beforeSnapshot,
                "Before snapshot cannot be null."
        );

        this.afterSnapshot = Objects.requireNonNull(
                afterSnapshot,
                "After snapshot cannot be null."
        );

        this.paymentAmount = Objects.requireNonNull(
                paymentAmount,
                "Payment amount cannot be null."
        );

        this.paymentProcessingState = paymentProcessingState;
        this.installmentOfferState = installmentOfferState;
        this.installmentAdhesionState = installmentAdhesionState;

        validateSameAccount(
                beforeSnapshot,
                afterSnapshot
        );
    }

    public FinancialScenario getScenario() {
        return scenario;
    }

    public FinancialSnapshot getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public FinancialSnapshot getAfterSnapshot() {
        return afterSnapshot;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public Optional<PaymentProcessingState> getPaymentProcessingState() {
        return Optional.ofNullable(
                paymentProcessingState
        );
    }

    public Optional<InstallmentOfferState> getInstallmentOfferState() {
        return Optional.ofNullable(
                installmentOfferState
        );
    }

    public Optional<InstallmentAdhesionState> getInstallmentAdhesionState() {
        return Optional.ofNullable(
                installmentAdhesionState
        );
    }

    private static void validateSameAccount(
            FinancialSnapshot before,
            FinancialSnapshot after
    ) {
        if (!before.getAccountId()
                .equals(after.getAccountId())) {

            throw new IllegalArgumentException(
                    "Before and after snapshots must belong to the same account."
            );
        }
    }
}