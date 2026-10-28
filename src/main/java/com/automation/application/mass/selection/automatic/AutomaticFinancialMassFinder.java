package com.automation.application.mass.selection.automatic;

import com.automation.application.mass.repository.FinancialMassRepository;
import com.automation.application.mass.selection.FinancialMassFinder;
import com.automation.domain.mass.FinancialMassSelectionMode;
import com.automation.domain.mass.record.FinancialMassRecord;
import com.automation.domain.mass.request.FinancialMassRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class AutomaticFinancialMassFinder
        implements FinancialMassFinder {

    private final FinancialMassRequest request;
    private final FinancialMassRepository repository;

    public AutomaticFinancialMassFinder(
            FinancialMassRequest request,
            FinancialMassRepository repository
    ) {
        this.request = requireAutomaticRequest(request);

        this.repository = Objects.requireNonNull(
                repository,
                "Financial mass repository cannot be null."
        );
    }

    @Override
    public List<FinancialMassRecord> find() {
        List<FinancialMassRecord> candidates =
                repository.findCandidates(
                        request.getCriteria(),
                        request.getQuantity()
                );

        List<FinancialMassRecord> validatedCandidates =
                validateRepositoryResult(candidates);

        if (validatedCandidates.size() < request.getQuantity()) {
            throw new IllegalStateException(
                    "Insufficient eligible financial mass. "
                            + "Requested: "
                            + request.getQuantity()
                            + ", found: "
                            + validatedCandidates.size()
                            + "."
            );
        }

        List<FinancialMassRecord> selectedRecords =
                new ArrayList<FinancialMassRecord>(
                        validatedCandidates.subList(
                                0,
                                request.getQuantity()
                        )
                );

        return Collections.unmodifiableList(selectedRecords);
    }

    private static FinancialMassRequest requireAutomaticRequest(
            FinancialMassRequest request
    ) {
        Objects.requireNonNull(
                request,
                "Financial mass request cannot be null."
        );

        if (request.getSelectionMode()
                != FinancialMassSelectionMode.AUTOMATIC) {

            throw new IllegalArgumentException(
                    "Automatic financial mass finder requires an AUTOMATIC request."
            );
        }

        return request;
    }

    private static List<FinancialMassRecord> validateRepositoryResult(
            List<FinancialMassRecord> records
    ) {
        Objects.requireNonNull(
                records,
                "Financial mass repository result cannot be null."
        );

        List<FinancialMassRecord> copiedRecords =
                new ArrayList<FinancialMassRecord>(records);

        if (copiedRecords.contains(null)) {
            throw new IllegalStateException(
                    "Financial mass repository result cannot contain null values."
            );
        }

        validateDuplicatedAccounts(copiedRecords);

        return copiedRecords;
    }

    private static void validateDuplicatedAccounts(
            List<FinancialMassRecord> records
    ) {
        Set<String> accountIds = new HashSet<String>();

        for (FinancialMassRecord record : records) {
            String accountId = record
                    .getAccount()
                    .getAccountId()
                    .getValue();

            if (!accountIds.add(accountId)) {
                throw new IllegalStateException(
                        "Financial mass repository returned duplicated account: "
                                + accountId
                );
            }
        }
    }
}