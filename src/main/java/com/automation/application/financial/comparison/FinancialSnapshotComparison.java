package com.automation.application.financial.comparison;

import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.math.BigDecimal;
import java.util.Objects;

public final class FinancialSnapshotComparison {

    private final FinancialSnapshot before;
    private final FinancialSnapshot after;

    public FinancialSnapshotComparison(
            FinancialSnapshot before,
            FinancialSnapshot after
    ) {
        this.before = Objects.requireNonNull(
                before,
                "Before snapshot cannot be null."
        );

        this.after = Objects.requireNonNull(
                after,
                "After snapshot cannot be null."
        );

        validateSameAccount();
    }

    public FinancialSnapshot getBefore() {
        return before;
    }

    public FinancialSnapshot getAfter() {
        return after;
    }

    public boolean statusChanged() {
        return before.getAccountStatus()
                != after.getAccountStatus();
    }

    public boolean balanceChanged() {
        return before.getCurrentBalance()
                .compareTo(after.getCurrentBalance()) != 0;
    }

    public BigDecimal balanceDifference() {
        return after.getCurrentBalance()
                .subtract(before.getCurrentBalance());
    }

    public boolean historyChanged() {
        return !before.getHistoryId()
                .equals(after.getHistoryId());
    }

    public boolean ticketChanged() {
        return !before.getTicketId()
                .equals(after.getTicketId());
    }

    private void validateSameAccount() {
        if (!before.getAccountId()
                .equals(after.getAccountId())) {

            throw new IllegalArgumentException(
                    "Financial snapshots must belong to the same account."
            );
        }
    }
}