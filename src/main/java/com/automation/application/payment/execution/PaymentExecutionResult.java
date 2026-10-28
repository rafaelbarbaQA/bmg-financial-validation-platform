package com.automation.application.payment.execution;

import com.automation.domain.account.AccountId;
import com.automation.domain.payment.instruction.PaymentInstruction;

import java.time.Instant;
import java.util.Objects;

public final class PaymentExecutionResult {

    private final PaymentInstruction instruction;
    private final PaymentExecutionStatus status;
    private final String externalPaymentId;
    private final String message;
    private final Instant executedAt;

    private PaymentExecutionResult(
            PaymentInstruction instruction,
            PaymentExecutionStatus status,
            String externalPaymentId,
            String message,
            Instant executedAt
    ) {
        this.instruction = Objects.requireNonNull(
                instruction,
                "Payment instruction cannot be null."
        );

        this.status = Objects.requireNonNull(
                status,
                "Payment execution status cannot be null."
        );

        this.externalPaymentId = normalizeOptionalText(
                externalPaymentId
        );

        this.message = requireText(
                message,
                "Payment execution message"
        );

        this.executedAt = Objects.requireNonNull(
                executedAt,
                "Payment execution date cannot be null."
        );

        validateResult();
    }

    public static PaymentExecutionResult success(
            PaymentInstruction instruction,
            String externalPaymentId,
            String message,
            Instant executedAt
    ) {
        return new PaymentExecutionResult(
                instruction,
                PaymentExecutionStatus.SUCCESS,
                externalPaymentId,
                message,
                executedAt
        );
    }

    public static PaymentExecutionResult failure(
            PaymentInstruction instruction,
            String message,
            Instant executedAt
    ) {
        return new PaymentExecutionResult(
                instruction,
                PaymentExecutionStatus.FAILURE,
                null,
                message,
                executedAt
        );
    }

    public PaymentInstruction getInstruction() {
        return instruction;
    }

    public AccountId getAccountId() {
        return instruction.getAccountId();
    }

    public PaymentExecutionStatus getStatus() {
        return status;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public boolean isSuccessful() {
        return status == PaymentExecutionStatus.SUCCESS;
    }

    private void validateResult() {
        if (status == PaymentExecutionStatus.SUCCESS
                && externalPaymentId == null) {

            throw new IllegalArgumentException(
                    "Successful payment execution must have an external payment ID."
            );
        }

        if (status == PaymentExecutionStatus.FAILURE
                && externalPaymentId != null) {

            throw new IllegalArgumentException(
                    "Failed payment execution cannot have an external payment ID."
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return normalizedValue;
    }

    private static String normalizeOptionalText(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }
}