package com.automation.application.financial.validation.validator;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.FinancialValidationStatus;
import com.automation.domain.payment.model.PaymentProcessingState;

public final class PaymentProcessingStatusValidator {

    public FinancialValidationResult validate(
            int actualStatus
    ) {
        boolean processed =
                actualStatus ==
                        PaymentProcessingState.PROCESSED_STATUS;

        if (processed) {
            return new FinancialValidationResult(
                    "PAYMENT_STATUS",
                    FinancialValidationStatus.PASS,
                    String.valueOf(
                            PaymentProcessingState.PROCESSED_STATUS
                    ),
                    String.valueOf(actualStatus),
                    "Payment was processed successfully."
            );
        }

        return new FinancialValidationResult(
                "PAYMENT_STATUS",
                FinancialValidationStatus.FAIL,
                String.valueOf(
                        PaymentProcessingState.PROCESSED_STATUS
                ),
                String.valueOf(actualStatus),
                "Payment was not processed successfully."
        );
    }
}