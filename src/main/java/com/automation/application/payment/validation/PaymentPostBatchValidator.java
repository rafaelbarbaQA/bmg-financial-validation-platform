package com.automation.application.payment.validation;

import com.automation.application.financial.comparison.FinancialSnapshotComparison;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.util.Objects;

public final class PaymentPostBatchValidator {

    public PaymentPostBatchValidationResult validate(
            PaymentValidationPlan plan,
            FinancialSnapshot after
    ) {
        Objects.requireNonNull(
                plan,
                "Payment validation plan cannot be null."
        );

        Objects.requireNonNull(
                after,
                "After snapshot cannot be null."
        );

        validateSameAccount(plan, after);

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        plan.getBeforeSnapshot(),
                        after
                );

        return new PaymentPostBatchValidationResult(
                plan.getBeforeSnapshot().getAccountId(),
                comparison.balanceChanged(),
                comparison.getBefore().getCurrentBalance(),
                comparison.getAfter().getCurrentBalance(),
                comparison.balanceDifference(),
                plan.getInstallmentExpectation()
        );
    }

    private static void validateSameAccount(
            PaymentValidationPlan plan,
            FinancialSnapshot after
    ) {
        if (!plan.getBeforeSnapshot()
                .getAccountId()
                .equals(after.getAccountId())) {

            throw new IllegalArgumentException(
                    "Payment validation plan and after snapshot must belong to the same account."
            );
        }
    }
}