package com.automation.application.payment.processing;

import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.repository.PaymentProcessingStateRepository;
import com.automation.domain.payment.model.PaymentProcessingState;

import java.util.Objects;

public final class PaymentProcessingStateService {

    private final PaymentProcessingStateRepository repository;

    public PaymentProcessingStateService(
            PaymentProcessingStateRepository repository
    ) {
        this.repository = Objects.requireNonNull(
                repository,
                "Payment processing state repository cannot be null."
        );
    }

    public PaymentProcessingState load(
            PaymentExecutionResult executionResult
    ) {
        Objects.requireNonNull(
                executionResult,
                "Payment execution result cannot be null."
        );

        if (!executionResult.isSuccessful()) {
            throw new IllegalStateException(
                    "Payment processing state cannot be loaded from a failed execution."
            );
        }

        long paymentEventId =
                parsePaymentEventId(
                        executionResult.getExternalPaymentId()
                );

        return repository
                .findByPaymentEventId(paymentEventId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Payment event "
                                        + paymentEventId
                                        + " was not found."
                        )
                );
    }

    private long parsePaymentEventId(
            String externalPaymentId
    ) {
        try {
            long paymentEventId =
                    Long.parseLong(
                            externalPaymentId
                    );

            if (paymentEventId <= 0) {
                throw new IllegalArgumentException(
                        "Payment event ID must be greater than zero."
                );
            }

            return paymentEventId;

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "External payment ID must contain the numeric Id_EventoPagamento. Actual value: "
                            + externalPaymentId,
                    exception
            );
        }
    }
}
