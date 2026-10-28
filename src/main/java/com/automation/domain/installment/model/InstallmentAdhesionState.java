package com.automation.domain.installment.model;

import com.automation.domain.installment.InstallmentExpectation;

import java.util.Objects;

public final class InstallmentAdhesionState {

    private final InstallmentExpectation observedOutcome;

    public InstallmentAdhesionState(
            InstallmentExpectation observedOutcome
    ) {
        this.observedOutcome = Objects.requireNonNull(
                observedOutcome,
                "Observed installment outcome cannot be null."
        );
    }

    public InstallmentExpectation getObservedOutcome() {
        return observedOutcome;
    }
}
