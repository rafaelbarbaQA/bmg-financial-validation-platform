package com.automation.application.payment.processing;

import com.automation.domain.payment.model.PaymentProcessingState;

import java.util.Objects;

public final class PaymentProcessingReadinessResolver {

    public PaymentProcessingReadiness resolve(
            PaymentProcessingState processingState
    ) {
        Objects.requireNonNull(
                processingState,
                "Payment processing state cannot be null."
        );

        if (processingState.isWaitingForBatch()) {
            return PaymentProcessingReadiness
                    .WAITING_FOR_BATCH;
        }

        if (processingState.isProcessed()) {
            return PaymentProcessingReadiness
                    .READY_FOR_POST_BATCH;
        }

        return PaymentProcessingReadiness
                .UNEXPECTED_STATUS;
    }
}