package com.automation.infrastructure.payment.execution;

import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.execution.PaymentExecutor;
import com.automation.domain.payment.instruction.PaymentInstruction;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class PreviewPaymentExecutor
        implements PaymentExecutor {

    private final Clock clock;

    public PreviewPaymentExecutor() {
        this(
                Clock.systemUTC()
        );
    }

    public PreviewPaymentExecutor(
            Clock clock
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "Clock cannot be null."
        );
    }

    @Override
    public PaymentExecutionResult execute(
            PaymentInstruction instruction
    ) {

        Objects.requireNonNull(
                instruction,
                "Payment instruction cannot be null."
        );

        Instant executedAt =
                clock.instant();

        String previewId =
                "PREVIEW-"
                        + instruction.getAccountId().getValue()
                        + "-"
                        + executedAt.toEpochMilli();

        return PaymentExecutionResult.success(
                instruction,
                previewId,
                "PREVIEW ONLY - no real payment was executed.",
                executedAt
        );
    }
}