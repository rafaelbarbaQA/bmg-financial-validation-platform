package com.automation.application.payment.scenario;

import com.automation.domain.financial.scenario.BelowMinimumPaymentScenario;
import com.automation.domain.financial.scenario.FinancialScenario;
import com.automation.domain.financial.scenario.FullPaymentScenario;
import com.automation.domain.financial.scenario.MinimumPaymentScenario;
import com.automation.domain.financial.scenario.Partial20PaymentScenario;
import com.automation.domain.financial.scenario.Partial60PaymentScenario;
import com.automation.domain.payment.PaymentScenario;

import java.util.Objects;

public final class FinancialScenarioPaymentMapper {

    public PaymentScenario map(
            FinancialScenario scenario
    ) {
        Objects.requireNonNull(
                scenario,
                "Financial scenario cannot be null."
        );

        if (scenario instanceof BelowMinimumPaymentScenario) {
            return PaymentScenario.BELOW_MINIMUM;
        }

        if (scenario instanceof MinimumPaymentScenario) {
            return PaymentScenario.MINIMUM;
        }

        if (scenario instanceof Partial20PaymentScenario) {
            return PaymentScenario
                    .BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT;
        }

        if (scenario instanceof Partial60PaymentScenario) {
            return PaymentScenario
                    .BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT;
        }

        if (scenario instanceof FullPaymentScenario) {
            return PaymentScenario.TOTAL;
        }

        throw new IllegalArgumentException(
                "Financial scenario is not a payment scenario: "
                        + scenario.getName()
        );
    }
}
