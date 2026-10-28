package com.automation.application.financial.runner;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.BelowMinimumPaymentScenario;
import com.automation.domain.financial.scenario.FullPaymentScenario;
import com.automation.domain.financial.scenario.MinimumPaymentScenario;
import com.automation.domain.financial.scenario.Partial20PaymentScenario;
import com.automation.domain.financial.scenario.Partial60PaymentScenario;

import java.util.Objects;

public final class FinancialScenarioRunner {

    private FinancialScenarioRunner() {
        throw new IllegalStateException(
                "FinancialScenarioRunner cannot be instantiated."
        );
    }

    public static PaymentScenarioExecution belowMinimumPayment(
            AccountId accountId
    ) {
        return new PaymentScenarioExecution(
                requireAccountId(accountId),
                new BelowMinimumPaymentScenario()
        );
    }

    public static PaymentScenarioExecution minimumPayment(
            AccountId accountId
    ) {
        return new PaymentScenarioExecution(
                requireAccountId(accountId),
                new MinimumPaymentScenario()
        );
    }

    public static PaymentScenarioExecution partialPayment20(
            AccountId accountId
    ) {
        return new PaymentScenarioExecution(
                requireAccountId(accountId),
                new Partial20PaymentScenario()
        );
    }

    public static PaymentScenarioExecution partialPayment60(
            AccountId accountId
    ) {
        return new PaymentScenarioExecution(
                requireAccountId(accountId),
                new Partial60PaymentScenario()
        );
    }

    public static PaymentScenarioExecution fullPayment(
            AccountId accountId
    ) {
        return new PaymentScenarioExecution(
                requireAccountId(accountId),
                new FullPaymentScenario()
        );
    }

    private static AccountId requireAccountId(
            AccountId accountId
    ) {
        return Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );
    }
}
