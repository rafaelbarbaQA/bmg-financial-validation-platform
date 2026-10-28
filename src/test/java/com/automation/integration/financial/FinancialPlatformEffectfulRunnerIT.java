package com.automation.integration.financial;

import org.testng.annotations.Test;

public final class FinancialPlatformEffectfulRunnerIT {

    @Test(groups = { "effectful" })
    public void runRealFinancialScenario() {

        String mode =
                System.getProperty(
                        "financial.mode",
                        ""
                )
                        .trim()
                        .toUpperCase();

        if (!"REAL".equals(mode)) {
            throw new IllegalArgumentException(
                    "FinancialPlatformEffectfulRunnerIT "
                            + "requires -Dfinancial.mode=REAL."
            );
        }

        new FinancialPlatformRunnerIT()
                .runFinancialScenario();
    }
}