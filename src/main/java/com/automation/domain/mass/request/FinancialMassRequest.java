package com.automation.domain.mass.request;

import com.automation.domain.mass.FinancialMassSelectionMode;
import com.automation.domain.mass.criteria.FinancialMassCriteria;

import java.util.Objects;

public final class FinancialMassRequest {

    private final int quantity;
    private final FinancialMassSelectionMode selectionMode;
    private final FinancialMassCriteria criteria;

    public FinancialMassRequest(
            int quantity,
            FinancialMassSelectionMode selectionMode,
            FinancialMassCriteria criteria
    ) {
        this.quantity = requirePositiveQuantity(quantity);

        this.selectionMode = Objects.requireNonNull(
                selectionMode,
                "Financial mass selection mode cannot be null."
        );

        this.criteria = Objects.requireNonNull(
                criteria,
                "Financial mass criteria cannot be null."
        );
    }

    public int getQuantity() {
        return quantity;
    }

    public FinancialMassSelectionMode getSelectionMode() {
        return selectionMode;
    }

    public FinancialMassCriteria getCriteria() {
        return criteria;
    }

    private static int requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Financial mass quantity must be greater than zero."
            );
        }

        return quantity;
    }
}