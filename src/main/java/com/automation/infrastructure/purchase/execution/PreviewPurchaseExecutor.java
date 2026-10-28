package com.automation.infrastructure.purchase.execution;

import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.execution.PurchaseExecutor;
import com.automation.domain.purchase.PurchaseInstruction;

import java.time.Clock;
import java.util.Objects;

public final class PreviewPurchaseExecutor
        implements PurchaseExecutor {

    private final Clock clock;

    public PreviewPurchaseExecutor() {
        this(
                Clock.systemUTC()
        );
    }

    public PreviewPurchaseExecutor(
            Clock clock
    ) {
        this.clock = Objects.requireNonNull(
                clock,
                "Clock cannot be null."
        );
    }

    @Override
    public PurchaseExecutionResult execute(
            PurchaseInstruction instruction
    ) {
        Objects.requireNonNull(
                instruction,
                "Purchase instruction cannot be null."
        );

        return PurchaseExecutionResult.success(
                instruction,
                "Purchase PREVIEW executed. No database operation was performed.",
                clock.instant()
        );
    }
}