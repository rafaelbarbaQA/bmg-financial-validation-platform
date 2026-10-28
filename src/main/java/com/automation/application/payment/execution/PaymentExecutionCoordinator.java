package com.automation.application.payment.execution;

import com.automation.domain.payment.instruction.PaymentInstruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PaymentExecutionCoordinator {

    private final PaymentExecutor paymentExecutor;

    public PaymentExecutionCoordinator(
            PaymentExecutor paymentExecutor
    ) {
        this.paymentExecutor = Objects.requireNonNull(
                paymentExecutor,
                "Payment executor cannot be null."
        );
    }

    public PaymentExecutionResult execute(
            PaymentInstruction instruction
    ) {
        Objects.requireNonNull(
                instruction,
                "Payment instruction cannot be null."
        );

        PaymentExecutionResult result =
                paymentExecutor.execute(instruction);

        if (result == null) {
            throw new IllegalStateException(
                    "Payment executor returned a null result."
            );
        }

        return result;
    }

    public List<PaymentExecutionResult> executeAll(
            List<PaymentInstruction> instructions
    ) {
        List<PaymentInstruction> validatedInstructions =
                requireValidInstructions(instructions);

        List<PaymentExecutionResult> results =
                new ArrayList<PaymentExecutionResult>();

        for (PaymentInstruction instruction : validatedInstructions) {
            PaymentExecutionResult result =
                    paymentExecutor.execute(instruction);

            if (result == null) {
                throw new IllegalStateException(
                        "Payment executor returned a null result."
                );
            }

            results.add(result);
        }

        return Collections.unmodifiableList(results);
    }

    private static List<PaymentInstruction> requireValidInstructions(
            List<PaymentInstruction> instructions
    ) {
        Objects.requireNonNull(
                instructions,
                "Payment instruction list cannot be null."
        );

        if (instructions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Payment instruction list cannot be empty."
            );
        }

        List<PaymentInstruction> copiedInstructions =
                new ArrayList<PaymentInstruction>(instructions);

        if (copiedInstructions.contains(null)) {
            throw new IllegalArgumentException(
                    "Payment instruction list cannot contain null values."
            );
        }

        return copiedInstructions;
    }
}