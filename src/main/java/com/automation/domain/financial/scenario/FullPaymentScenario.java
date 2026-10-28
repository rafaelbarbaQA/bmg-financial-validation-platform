package com.automation.domain.financial.scenario;

import com.automation.domain.installment.InstallmentExpectation;

public final class FullPaymentScenario
        extends FinancialScenario {

    @Override
    public String getName() {
        return "FULL_PAYMENT";
    }

    @Override
    public InstallmentExpectation expectedInstallment() {
        return InstallmentExpectation.NO_INSTALLMENT_ADHESION;
    }
}