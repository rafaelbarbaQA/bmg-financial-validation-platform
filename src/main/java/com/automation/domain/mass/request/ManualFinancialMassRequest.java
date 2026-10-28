package com.automation.domain.mass.request;

import com.automation.domain.account.AccountId;
import com.automation.domain.mass.FinancialMassSelectionMode;
import com.automation.domain.mass.criteria.FinancialMassCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ManualFinancialMassRequest {

    private final FinancialMassRequest massRequest;
    private final List<AccountId> accountIds;

    public ManualFinancialMassRequest(
            FinancialMassCriteria criteria,
            List<AccountId> accountIds
    ) {
        this.accountIds = requireValidAccountIds(accountIds);

        this.massRequest = new FinancialMassRequest(
                this.accountIds.size(),
                FinancialMassSelectionMode.MANUAL,
                Objects.requireNonNull(
                        criteria,
                        "Financial mass criteria cannot be null."
                )
        );
    }

    public FinancialMassRequest getMassRequest() {
        return massRequest;
    }

    public List<AccountId> getAccountIds() {
        return accountIds;
    }

    private static List<AccountId> requireValidAccountIds(
            List<AccountId> accountIds
    ) {
        Objects.requireNonNull(
                accountIds,
                "Manual account list cannot be null."
        );

        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Manual account list cannot be empty."
            );
        }

        List<AccountId> copiedAccountIds =
                new ArrayList<AccountId>(accountIds);

        if (copiedAccountIds.contains(null)) {
            throw new IllegalArgumentException(
                    "Manual account list cannot contain null values."
            );
        }

        Set<AccountId> uniqueAccountIds =
                new HashSet<AccountId>(copiedAccountIds);

        if (uniqueAccountIds.size() != copiedAccountIds.size()) {
            throw new IllegalArgumentException(
                    "Manual account list cannot contain duplicated accounts."
            );
        }

        return Collections.unmodifiableList(copiedAccountIds);
    }
}