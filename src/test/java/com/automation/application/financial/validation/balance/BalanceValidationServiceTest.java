package com.automation.application.financial.validation.balance;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.FinancialValidationStatus;
import com.automation.application.financial.validation.validator.BalanceValidator;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public final class BalanceValidationServiceTest {

    private final BalanceValidationService service =
            new BalanceValidationService(
                    new FullPaymentBalanceExpectationCalculator(),
                    new BalanceValidator()
            );

    @Test
    public void shouldPassWhenFullPaymentClearsBalance() {

        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        "0.00"
                );

        FinancialValidationResult result =
                service.validate(
                        before,
                        after,
                        new BigDecimal("1000.00")
                );

        assertTrue(result.passed());

        assertFalse(result.failed());

        assertEquals(
                result.getStatus(),
                FinancialValidationStatus.PASS
        );

        assertEquals(
                result.getRule(),
                "BALANCE"
        );

        assertEquals(
                result.getExpected(),
                "0.00"
        );

        assertEquals(
                result.getActual(),
                "0.00"
        );
    }

    @Test
    public void shouldFailWhenFullPaymentDoesNotClearBalance() {

        FinancialSnapshot before =
                createSnapshot(
                        "13714323",
                        "1000.00"
                );

        FinancialSnapshot after =
                createSnapshot(
                        "13714323",
                        "53.42"
                );

        FinancialValidationResult result =
                service.validate(
                        before,
                        after,
                        new BigDecimal("1000.00")
                );

        assertTrue(result.failed());

        assertFalse(result.passed());

        assertEquals(
                result.getStatus(),
                FinancialValidationStatus.FAIL
        );

        assertEquals(
                result.getExpected(),
                "0.00"
        );

        assertEquals(
                result.getActual(),
                "53.42"
        );
    }

    private FinancialSnapshot createSnapshot(
            String accountId,
            String currentBalance
    ) {
        return new FinancialSnapshot(
                new AccountId(accountId),
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
