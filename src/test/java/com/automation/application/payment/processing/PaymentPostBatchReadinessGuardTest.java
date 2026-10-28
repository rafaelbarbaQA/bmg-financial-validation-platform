package com.automation.application.payment.processing;

import com.automation.domain.payment.model.PaymentProcessingState;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class PaymentPostBatchReadinessGuardTest {

    private final PaymentPostBatchReadinessGuard guard =
            new PaymentPostBatchReadinessGuard();

    @Test
    public void shouldAllowProcessedPayment() {

        guard.requireReady(
                new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
        );
    }

    @Test
    public void shouldRejectPaymentWaitingForBatch() {

        IllegalStateException exception =
                expectThrows(
                        IllegalStateException.class,
                        () -> guard.requireReady(
                                new PaymentProcessingState(PaymentProcessingState.WAITING_FOR_BATCH_STATUS)
                        )
                );

        assertEquals(
                exception.getMessage(),
                "Payment is waiting for batch processing. Current status: 0."
        );
    }

    @Test
    public void shouldRejectUnexpectedStatus() {

        IllegalStateException exception =
                expectThrows(
                        IllegalStateException.class,
                        () -> guard.requireReady(
                                new PaymentProcessingState(99)
                        )
                );

        assertEquals(
                exception.getMessage(),
                "Payment has an unexpected processing status: 99."
        );
    }

    @Test
    public void shouldExposeReadinessWithoutThrowing() {

        PaymentProcessingReadiness readiness =
                guard.evaluate(
                        new PaymentProcessingState(PaymentProcessingState.WAITING_FOR_BATCH_STATUS)
                );

        assertEquals(
                readiness,
                PaymentProcessingReadiness.WAITING_FOR_BATCH
        );
    }
}