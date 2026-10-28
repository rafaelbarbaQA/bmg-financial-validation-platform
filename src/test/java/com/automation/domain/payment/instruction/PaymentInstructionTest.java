package com.automation.domain.payment.instruction;

import com.automation.domain.account.AccountId;
import com.automation.domain.payment.PaymentScenario;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.expectThrows;

public final class PaymentInstructionTest {

    @Test
    public void shouldCreateReadyPaymentInstruction() {
        AccountId accountId =
                new AccountId("2310664");

        Instant preparedAt =
                Instant.parse("2026-08-04T21:00:00Z");

        PaymentInstruction instruction =
                new PaymentInstruction(
                        accountId,
                        PaymentScenario.MINIMUM,
                        new BigDecimal("200.00"),
                        preparedAt
                );

        assertSame(
                instruction.getAccountId(),
                accountId
        );

        assertEquals(
                instruction.getScenario(),
                PaymentScenario.MINIMUM
        );

        assertEquals(
                instruction.getAmount(),
                new BigDecimal("200.00")
        );

        assertEquals(
                instruction.getPreparedAt(),
                preparedAt
        );
}

    @Test
    public void shouldRejectZeroAmount() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new PaymentInstruction(
                        new AccountId("2310664"),
                        PaymentScenario.BELOW_MINIMUM,
                        BigDecimal.ZERO,
                        Instant.now()
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment amount must be greater than zero."
        );
    }
}