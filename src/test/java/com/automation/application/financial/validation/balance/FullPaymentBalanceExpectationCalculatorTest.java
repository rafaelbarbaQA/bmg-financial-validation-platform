package com.automation.application.financial.validation.balance;

import com.automation.application.financial.validation.BalanceExpectation;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class FullPaymentBalanceExpectationCalculatorTest {

    private final FullPaymentBalanceExpectationCalculator calculator =
            new FullPaymentBalanceExpectationCalculator();

    @Test
    public void shouldExpectZeroBalanceForFullPayment() {

        FinancialSnapshot before =
                createSnapshot("1000.00");

        BalanceExpectation expectation =
                calculator.calculate(
                        before,
                        new BigDecimal("1000.00")
                );

        assertEquals(
                expectation.getExpectedBalance()
                        .compareTo(BigDecimal.ZERO),
                0
        );
    }

    @Test
    public void shouldRejectPaymentDifferentFromCurrentBalance() {

        FinancialSnapshot before =
                createSnapshot("1000.00");

        expectThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        before,
                        new BigDecimal("900.00")
                )
        );
    }

    private FinancialSnapshot createSnapshot(
            String currentBalance
    ) {
        return new FinancialSnapshot(
                new AccountId("13714323"),
                0,
                "877716729",
                "568338116",
                "9268419047",
                new BigDecimal(currentBalance),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 10),
                Instant.parse("2026-08-09T12:00:00Z")
        );
    }
}
