package com.automation.application.purchase.execution;

import com.automation.domain.purchase.PurchaseInstruction;

import java.time.Instant;
import java.util.Objects;

public final class PurchaseExecutionResult {

    private final PurchaseInstruction instruction;
    private final boolean successful;
    private final String message;
    private final Instant executedAt;

    private PurchaseExecutionResult(
            PurchaseInstruction instruction,
            boolean successful,
            String message,
            Instant executedAt
    ) {
        this.instruction = Objects.requireNonNull(
                instruction,
                "Purchase instruction cannot be null."
        );

        this.successful = successful;

        this.message =
                requireText(
                        message,
                        "Purchase execution message"
                );

        this.executedAt = Objects.requireNonNull(
                executedAt,
                "Purchase execution date cannot be null."
        );
    }

    public static PurchaseExecutionResult success(
            PurchaseInstruction instruction,
            String message,
            Instant executedAt
    ) {
        return new PurchaseExecutionResult(
                instruction,
                true,
                message,
                executedAt
        );
    }

    public static PurchaseExecutionResult failure(
            PurchaseInstruction instruction,
            String message,
            Instant executedAt
    ) {
        return new PurchaseExecutionResult(
                instruction,
                false,
                message,
                executedAt
        );
    }

    public PurchaseInstruction getInstruction() {
        return instruction;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getMessage() {
        return message;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        String normalized =
                value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return normalized;
    }
}