package com.automation.application.payment.execution;

import com.automation.domain.payment.instruction.PaymentInstruction;

public interface PaymentExecutor {

    PaymentExecutionResult execute(
            PaymentInstruction instruction
    );
}