package com.automation.domain.financial.scenario;

import com.automation.domain.installment.InstallmentExpectation;

public final class MinimumPaymentScenario
        extends FinancialScenario {

    @Override
    public String getName() {
        return "MINIMUM_PAYMENT";
    }

    @Override
    public InstallmentExpectation expectedInstallment() {
        return InstallmentExpectation.ORIGINAL_OFFER_ADHESION;
    }
}