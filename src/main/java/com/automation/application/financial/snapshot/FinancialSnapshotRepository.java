package com.automation.application.financial.snapshot;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

public interface FinancialSnapshotRepository {

    FinancialSnapshot capture(
            AccountId accountId
    );
}