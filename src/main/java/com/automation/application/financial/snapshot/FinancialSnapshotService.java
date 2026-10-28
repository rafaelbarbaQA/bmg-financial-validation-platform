package com.automation.application.financial.snapshot;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.util.Objects;

public final class FinancialSnapshotService {

    private final FinancialSnapshotRepository repository;

    public FinancialSnapshotService(
            FinancialSnapshotRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "Financial snapshot repository cannot be null."
        );
    }

    public FinancialSnapshot capture(
            AccountId accountId
    ) {
        Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        FinancialSnapshot snapshot =
                repository.capture(accountId);

        if (snapshot == null) {
            throw new IllegalStateException(
                    "Financial snapshot repository returned null for account "
                            + accountId.getValue()
                            + "."
            );
        }

        return snapshot;
    }
}