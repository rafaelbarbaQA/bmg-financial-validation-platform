package com.automation.application.payment.execution;

import com.automation.domain.account.AccountId;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public final class PaymentExecutionResultTest {

    private static final Instant PREPARED_AT =
            Instant.parse("2026-08-04T21:00:00Z");

    private static final Instant EXECUTED_AT =
            Instant.parse("2026-08-04T21:05:00Z");

    private PaymentInstruction instruction;

    @BeforeMethod
    public void setUp() {
        instruction = new PaymentInstruction(
                new AccountId("2310664"),
                PaymentScenario.MINIMUM,
                new BigDecimal("200.00"),
                PREPARED_AT
        );
    }

    @Test
    public void shouldCreateSuccessfulExecutionResult() {
        PaymentExecutionResult result =
                PaymentExecutionResult.success(
                        instruction,
                        "987654321",
                        "Pagamento inserido com sucesso.",
                        EXECUTED_AT
                );

        assertSame(
                result.getInstruction(),
                instruction
        );

        assertEquals(
                result.getAccountId(),
                new AccountId("2310664")
        );

        assertEquals(
                result.getStatus(),
                PaymentExecutionStatus.SUCCESS
        );

        assertEquals(
                result.getExternalPaymentId(),
                "987654321"
        );

        assertEquals(
                result.getMessage(),
                "Pagamento inserido com sucesso."
        );

        assertEquals(
                result.getExecutedAt(),
                EXECUTED_AT
        );

        assertTrue(result.isSuccessful());
    }

    @Test
    public void shouldCreateFailedExecutionResult() {
        PaymentExecutionResult result =
                PaymentExecutionResult.failure(
                        instruction,
                        "Falha ao inserir o pagamento.",
                        EXECUTED_AT
                );

        assertSame(
                result.getInstruction(),
                instruction
        );

        assertEquals(
                result.getStatus(),
                PaymentExecutionStatus.FAILURE
        );

        assertNull(result.getExternalPaymentId());

        assertEquals(
                result.getMessage(),
                "Falha ao inserir o pagamento."
        );

        assertEquals(
                result.getExecutedAt(),
                EXECUTED_AT
        );

        assertFalse(result.isSuccessful());
    }

    @Test
    public void shouldNormalizeSuccessFields() {
        PaymentExecutionResult result =
                PaymentExecutionResult.success(
                        instruction,
                        " 987654321 ",
                        " Pagamento inserido com sucesso. ",
                        EXECUTED_AT
                );

        assertEquals(
                result.getExternalPaymentId(),
                "987654321"
        );

        assertEquals(
                result.getMessage(),
                "Pagamento inserido com sucesso."
        );
    }

    @Test
    public void shouldRejectSuccessWithoutExternalPaymentId() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> PaymentExecutionResult.success(
                        instruction,
                        " ",
                        "Pagamento inserido com sucesso.",
                        EXECUTED_AT
                )
        );

        assertEquals(
                exception.getMessage(),
                "Successful payment execution must have an external payment ID."
        );
    }

    @Test
    public void shouldRejectBlankExecutionMessage() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> PaymentExecutionResult.failure(
                        instruction,
                        " ",
                        EXECUTED_AT
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment execution message cannot be blank."
        );
    }

    @Test
    public void shouldRejectNullInstruction() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> PaymentExecutionResult.success(
                        null,
                        "987654321",
                        "Pagamento inserido com sucesso.",
                        EXECUTED_AT
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment instruction cannot be null."
        );
    }

    @Test
    public void shouldRejectNullExecutionDate() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> PaymentExecutionResult.failure(
                        instruction,
                        "Falha ao inserir o pagamento.",
                        null
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment execution date cannot be null."
        );
    }
}