package com.automation.application.payment.validation;

import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.installment.InstallmentExpectation;
import com.automation.domain.installment.InstallmentExpectationResolver;
import com.automation.domain.payment.instruction.PaymentInstruction;

import java.util.Objects;

public final class PaymentValidationPlanFactory {

    private final InstallmentExpectationResolver installmentResolver;

    public PaymentValidationPlanFactory() {
        this(new InstallmentExpectationResolver());
    }

    public PaymentValidationPlanFactory(
            InstallmentExpectationResolver installmentResolver
    ) {
        this.installmentResolver = Objects.requireNonNull(
                installmentResolver,
                "Installment expectation resolver cannot be null."
        );
    }

    public PaymentValidationPlan create(
            FinancialSnapshot before,
            PaymentInstruction payment
    ) {
        Objects.requireNonNull(
                before,
                "Before snapshot cannot be null."
        );

        Objects.requireNonNull(
                payment,
                "Payment instruction cannot be null."
        );

        validateSameAccount(before, payment);

        InstallmentExpectation installmentExpectation =
                installmentResolver.resolve(
                        payment.getAmount(),
                        before.getMinimumAmount(),
                        before.getInvoiceAmount()
                );

        return new PaymentValidationPlan(
                before,
                payment.getScenario(),
                payment.getAmount(),
                installmentExpectation
        );
    }

    private static void validateSameAccount(
            FinancialSnapshot before,
            PaymentInstruction payment
    ) {
        if (!before.getAccountId()
                .equals(payment.getAccountId())) {

            throw new IllegalArgumentException(
                    "Snapshot and payment instruction must belong to the same account."
            );
        }
    }
}