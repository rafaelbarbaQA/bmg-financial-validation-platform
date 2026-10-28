package com.automation.application.payment.processing;

import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.repository.PaymentProcessingStateRepository;
import com.automation.domain.account.AccountId;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.PaymentProcessingState;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class PaymentProcessingStateServiceTest {

    @Test
    public void shouldLoadPaymentStatusUsingExternalPaymentEventId() {

        PaymentProcessingStateRepository repository =
                paymentEventId -> {
                    assertEquals(
                            paymentEventId,
                            987654321L
                    );

                    return Optional.of(
                            new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                    );
                };

        PaymentProcessingStateService service =
                new PaymentProcessingStateService(
                        repository
                );

        PaymentProcessingState state =
                service.load(
                        successfulExecution(
                                "987654321"
                        )
                );

        assertEquals(
                state.getStatus(),
                2
        );
    }

    @Test
    public void shouldRejectNonNumericExternalPaymentId() {

        PaymentProcessingStateService service =
                new PaymentProcessingStateService(
                        paymentEventId ->
                                Optional.of(
                                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                                )
                );

        expectThrows(
                IllegalStateException.class,
                () -> service.load(
                        successfulExecution(
                                "PAY-2310664"
                        )
                )
        );
    }

    @Test
    public void shouldFailWhenPaymentEventDoesNotExist() {

        PaymentProcessingStateService service =
                new PaymentProcessingStateService(
                        paymentEventId ->
                                Optional.empty()
                );

        expectThrows(
                IllegalStateException.class,
                () -> service.load(
                        successfulExecution(
                                "987654321"
                        )
                )
        );
    }

    private PaymentExecutionResult successfulExecution(
            String externalPaymentId
    ) {

        PaymentInstruction instruction =
                new PaymentInstruction(
                        new AccountId("2310664"),
                        PaymentScenario.MINIMUM,
                        new BigDecimal("200.00"),
                        Instant.parse(
                                "2026-08-09T12:00:00Z"
                        )
                );

        return PaymentExecutionResult.success(
                instruction,
                externalPaymentId,
                "Payment inserted successfully.",
                Instant.parse(
                        "2026-08-09T12:01:00Z"
                )
        );
    }
}
