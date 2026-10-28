package com.automation.application.payment.processing;

import com.automation.domain.payment.model.PaymentProcessingState;

import java.util.Objects;

public final class PaymentPostBatchReadinessGuard {

    private final PaymentProcessingReadinessResolver resolver;

    public PaymentPostBatchReadinessGuard() {
        this(
                new PaymentProcessingReadinessResolver()
        );
    }

    public PaymentPostBatchReadinessGuard(
            PaymentProcessingReadinessResolver resolver
    ) {

        this.resolver =
                Objects.requireNonNull(
                        resolver,
                        "Payment processing readiness resolver cannot be null."
                );
    }

    public PaymentProcessingReadiness evaluate(
            PaymentProcessingState processingState
    ) {

        return resolver.resolve(
                processingState
        );
    }

    public void requireReady(
            PaymentProcessingState processingState
    ) {

        PaymentProcessingReadiness readiness =
                evaluate(
                        processingState
                );

        if (
                readiness ==
                        PaymentProcessingReadiness.READY_FOR_POST_BATCH
        ) {
            return;
        }

        if (
                readiness ==
                        PaymentProcessingReadiness.WAITING_FOR_BATCH
        ) {

            throw new IllegalStateException(
                    "Payment is waiting for batch processing. "
                            + "Current status: "
                            + processingState.getStatus()
                            + "."
            );
        }

        throw new IllegalStateException(
                "Payment has an unexpected processing status: "
                        + processingState.getStatus()
                        + "."
        );
    }
}