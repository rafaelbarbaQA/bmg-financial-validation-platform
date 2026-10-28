package com.automation.application.mass.selection.manual;

import com.automation.application.mass.selection.FinancialMassFinder;
import com.automation.domain.mass.record.FinancialMassRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ManualFinancialMassFinder
        implements FinancialMassFinder {

    private final List<FinancialMassRecord> massRecords;

    public ManualFinancialMassFinder(
            List<FinancialMassRecord> massRecords
    ) {
        this.massRecords = requireValidMassRecords(
                massRecords
        );

        validateDuplicatedAccounts(this.massRecords);
    }

    @Override
    public List<FinancialMassRecord> find() {
        return massRecords;
    }

    private static List<FinancialMassRecord> requireValidMassRecords(
            List<FinancialMassRecord> massRecords
    ) {
        Objects.requireNonNull(
                massRecords,
                "Manual financial mass list cannot be null."
        );

        if (massRecords.isEmpty()) {
            throw new IllegalArgumentException(
                    "Manual financial mass list cannot be empty."
            );
        }

        List<FinancialMassRecord> copiedRecords =
                new ArrayList<FinancialMassRecord>(massRecords);

        if (copiedRecords.contains(null)) {
            throw new IllegalArgumentException(
                    "Manual financial mass list cannot contain null values."
            );
        }

        return Collections.unmodifiableList(copiedRecords);
    }

    private static void validateDuplicatedAccounts(
            List<FinancialMassRecord> massRecords
    ) {
        Set<String> accountIds = new HashSet<String>();

        for (FinancialMassRecord massRecord : massRecords) {
            String accountId = massRecord
                    .getAccount()
                    .getAccountId()
                    .getValue();

            if (!accountIds.add(accountId)) {
                throw new IllegalArgumentException(
                        "Manual financial mass list cannot contain duplicated accounts: "
                                + accountId
                );
            }
        }
    }
}