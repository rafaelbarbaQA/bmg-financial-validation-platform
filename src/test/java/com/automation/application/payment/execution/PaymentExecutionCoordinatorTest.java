package com.automation.application.payment.execution;

import com.automation.domain.account.AccountId;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public final class PaymentExecutionCoordinatorTest {

    private static final Instant PREPARED_AT =
            Instant.parse("2026-08-04T21:00:00Z");

    private static final Instant EXECUTED_AT =
            Instant.parse("2026-08-04T21:05:00Z");

    private PaymentExecutionCoordinator coordinator;

    @BeforeMethod
    public void setUp() {
        coordinator = new PaymentExecutionCoordinator(
                new SuccessfulFakePaymentExecutor()
        );
    }

    @Test
    public void shouldExecuteSinglePaymentInstruction() {
        PaymentInstruction instruction =
                createInstruction(
                        "2310664",
                        "200.00"
                );

        PaymentExecutionResult result =
                coordinator.execute(instruction);

        assertTrue(result.isSuccessful());

        assertEquals(
                result.getAccountId(),
                new AccountId("2310664")
        );

        assertEquals(
                result.getExternalPaymentId(),
                "PAY-2310664"
        );
    }

    @Test
    public void shouldExecuteMultiplePaymentInstructions() {
        PaymentInstruction firstInstruction =
                createInstruction(
                        "2310664",
                        "200.00"
                );

        PaymentInstruction secondInstruction =
                createInstruction(
                        "2310665",
                        "100.00"
                );

        List<PaymentExecutionResult> results =
                coordinator.executeAll(
                        Arrays.asList(
                                firstInstruction,
                                secondInstruction
                        )
                );

        assertEquals(results.size(), 2);

        assertEquals(
                results.get(0).getExternalPaymentId(),
                "PAY-2310664"
        );

        assertEquals(
                results.get(1).getExternalPaymentId(),
                "PAY-2310665"
        );

        assertTrue(results.get(0).isSuccessful());
        assertTrue(results.get(1).isSuccessful());
    }

    @Test
    public void shouldPreserveFailureResult() {
        PaymentExecutionCoordinator failureCoordinator =
                new PaymentExecutionCoordinator(
                        new FailedFakePaymentExecutor()
                );

        PaymentExecutionResult result =
                failureCoordinator.execute(
                        createInstruction(
                                "2310664",
                                "200.00"
                        )
                );

        assertFalse(result.isSuccessful());

        assertEquals(
                result.getMessage(),
                "Falha simulada ao inserir pagamento."
        );
    }

    @Test
    public void shouldReturnImmutableResultList() {
        List<PaymentExecutionResult> results =
                coordinator.executeAll(
                        Collections.singletonList(
                                createInstruction(
                                        "2310664",
                                        "200.00"
                                )
                        )
                );

        expectThrows(
                UnsupportedOperationException.class,
                () -> results.add(results.get(0))
        );
    }

    @Test
    public void shouldRejectEmptyInstructionList() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> coordinator.executeAll(
                        Collections.emptyList()
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment instruction list cannot be empty."
        );
    }

    @Test
    public void shouldRejectInstructionListContainingNull() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> coordinator.executeAll(
                        Arrays.asList(
                                createInstruction(
                                        "2310664",
                                        "200.00"
                                ),
                                null
                        )
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment instruction list cannot contain null values."
        );
    }

    @Test
    public void shouldRejectExecutorReturningNullResult() {
        PaymentExecutionCoordinator invalidCoordinator =
                new PaymentExecutionCoordinator(
                        instruction -> null
                );

        IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> invalidCoordinator.executeAll(
                        Collections.singletonList(
                                createInstruction(
                                        "2310664",
                                        "200.00"
                                )
                        )
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment executor returned a null result."
        );
    }

    @Test
    public void shouldRejectNullExecutor() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> new PaymentExecutionCoordinator(null)
        );

        assertEquals(
                exception.getMessage(),
                "Payment executor cannot be null."
        );
    }

    private PaymentInstruction createInstruction(
            String accountId,
            String amount
    ) {
        return new PaymentInstruction(
                new AccountId(accountId),
                PaymentScenario.MINIMUM,
                new BigDecimal(amount),
                PREPARED_AT
        );
    }

    private static final class SuccessfulFakePaymentExecutor
            implements PaymentExecutor {

        @Override
        public PaymentExecutionResult execute(
                PaymentInstruction instruction
        ) {
            return PaymentExecutionResult.success(
                    instruction,
                    "PAY-" + instruction
                            .getAccountId()
                            .getValue(),
                    "Pagamento inserido com sucesso.",
                    EXECUTED_AT
            );
        }
    }

    private static final class FailedFakePaymentExecutor
            implements PaymentExecutor {

        @Override
        public PaymentExecutionResult execute(
                PaymentInstruction instruction
        ) {
            return PaymentExecutionResult.failure(
                    instruction,
                    "Falha simulada ao inserir pagamento.",
                    EXECUTED_AT
            );
        }
    }
}