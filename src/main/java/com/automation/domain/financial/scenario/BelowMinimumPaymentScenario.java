package com.automation.domain.financial.scenario;

import com.automation.domain.installment.InstallmentExpectation;

public final class BelowMinimumPaymentScenario
        extends FinancialScenario {

    @Override
    public String getName() {
        return "BELOW_MINIMUM_PAYMENT";
    }

    @Override
    public InstallmentExpectation expectedInstallment() {
        return InstallmentExpectation.NO_INSTALLMENT_ADHESION;
    }
}