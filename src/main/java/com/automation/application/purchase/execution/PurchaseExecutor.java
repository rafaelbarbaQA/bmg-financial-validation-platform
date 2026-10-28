package com.automation.application.purchase.execution;

import com.automation.domain.purchase.PurchaseInstruction;

@FunctionalInterface
public interface PurchaseExecutor {

    PurchaseExecutionResult execute(
            PurchaseInstruction instruction
    );
}