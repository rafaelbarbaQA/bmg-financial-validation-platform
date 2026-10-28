package com.automation.application.payment.processing;

import com.automation.domain.payment.model.PaymentProcessingState;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public final class PaymentProcessingReadinessResolverTest {

    private final PaymentProcessingReadinessResolver resolver =
            new PaymentProcessingReadinessResolver();

    @Test
    public void shouldIdentifyPaymentWaitingForBatch() {

        PaymentProcessingReadiness readiness =
                resolver.resolve(
                        new PaymentProcessingState(PaymentProcessingState.WAITING_FOR_BATCH_STATUS)
                );

        assertEquals(
                readiness,
                PaymentProcessingReadiness.WAITING_FOR_BATCH
        );
    }

    @Test
    public void shouldIdentifyPaymentReadyForPostBatch() {

        PaymentProcessingReadiness readiness =
                resolver.resolve(
                        new PaymentProcessingState(PaymentProcessingState.PROCESSED_STATUS)
                );

        assertEquals(
                readiness,
                PaymentProcessingReadiness.READY_FOR_POST_BATCH
        );
    }

    @Test
    public void shouldIdentifyUnexpectedProcessingStatus() {

        PaymentProcessingReadiness readiness =
                resolver.resolve(
                        new PaymentProcessingState(99)
                );

        assertEquals(
                readiness,
                PaymentProcessingReadiness.UNEXPECTED_STATUS
        );
    }

    @Test(
            expectedExceptions = NullPointerException.class
    )
    public void shouldRejectNullProcessingState() {

        resolver.resolve(null);
    }
}