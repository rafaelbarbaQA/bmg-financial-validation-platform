package com.automation.domain.installment.model;

public final class InstallmentOfferState {

    private final boolean offerAvailable;

    public InstallmentOfferState(
            boolean offerAvailable
    ) {
        this.offerAvailable = offerAvailable;
    }

    public boolean isOfferAvailable() {
        return offerAvailable;
    }
}
