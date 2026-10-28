package com.automation.application.purchase.execution;

import com.automation.domain.purchase.PurchaseInstruction;

@FunctionalInterface
public interface PurchaseProcedureGateway {

    void execute(
            PurchaseInstruction instruction
    );
}