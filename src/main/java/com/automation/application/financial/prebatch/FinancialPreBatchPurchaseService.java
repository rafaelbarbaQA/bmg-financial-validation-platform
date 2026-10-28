package com.automation.application.financial.prebatch;

import com.automation.application.purchase.execution.PurchaseExecutionCoordinator;
import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;

import java.util.Objects;

public final class FinancialPreBatchPurchaseService {

    private final PurchaseInstructionGenerator instructionGenerator;
    private final PurchaseExecutionCoordinator executionCoordinator;

    public FinancialPreBatchPurchaseService(
            PurchaseInstructionGenerator instructionGenerator,
            PurchaseExecutionCoordinator executionCoordinator
    ) {
        this.instructionGenerator =
                Objects.requireNonNull(
                        instructionGenerator,
                        "Purchase instruction generator cannot be null."
                );

        this.executionCoordinator =
                Objects.requireNonNull(
                        executionCoordinator,
                        "Purchase execution coordinator cannot be null."
                );
    }

    public PurchaseExecutionResult execute(
            AccountId accountId,
            PurchaseOperation operation
    ) {
        Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        Objects.requireNonNull(
                operation,
                "Purchase operation cannot be null."
        );

        PurchaseInstruction instruction =
                instructionGenerator.generate(
                        accountId,
                        operation
                );

        return executionCoordinator.execute(
                instruction
        );
    }
}