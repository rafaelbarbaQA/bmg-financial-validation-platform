package com.automation.domain.financial.scenario;

import com.automation.domain.installment.InstallmentExpectation;

public final class Partial60PaymentScenario
        extends FinancialScenario {

    @Override
    public String getName() {
        return "PARTIAL_PAYMENT_60_PERCENT";
    }

    @Override
    public InstallmentExpectation expectedInstallment() {
        return InstallmentExpectation.RECALCULATED_OFFER_ADHESION;
    }
}