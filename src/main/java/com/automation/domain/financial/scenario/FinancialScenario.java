package com.automation.domain.financial.scenario;

import com.automation.domain.installment.InstallmentExpectation;

public abstract class FinancialScenario {

    public abstract String getName();

    public abstract InstallmentExpectation expectedInstallment();
}