package com.automation.application.purchase.execution;

import com.automation.domain.purchase.PurchaseInstruction;

import java.util.Objects;

public final class PurchaseExecutionCoordinator {

    private final PurchaseExecutor purchaseExecutor;

    public PurchaseExecutionCoordinator(
            PurchaseExecutor purchaseExecutor
    ) {
        this.purchaseExecutor =
                Objects.requireNonNull(
                        purchaseExecutor,
                        "Purchase executor cannot be null."
                );
    }

    public PurchaseExecutionResult execute(
            PurchaseInstruction instruction
    ) {
        Objects.requireNonNull(
                instruction,
                "Purchase instruction cannot be null."
        );

        PurchaseExecutionResult result =
                purchaseExecutor.execute(
                        instruction
                );

        if (result == null) {
            throw new IllegalStateException(
                    "Purchase executor returned a null result."
            );
        }

        return result;
    }
}