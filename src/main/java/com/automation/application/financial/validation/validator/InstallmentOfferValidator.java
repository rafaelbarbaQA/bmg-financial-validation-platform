package com.automation.application.financial.validation.validator;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.FinancialValidationStatus;

public final class InstallmentOfferValidator {

    public FinancialValidationResult validate(
            boolean expectedOffer,
            boolean actualOffer
    ) {
        boolean matches =
                expectedOffer == actualOffer;

        return new FinancialValidationResult(
                "INSTALLMENT_OFFER",
                matches
                        ? FinancialValidationStatus.PASS
                        : FinancialValidationStatus.FAIL,
                String.valueOf(expectedOffer),
                String.valueOf(actualOffer),
                matches
                        ? "Installment offer matches expected state."
                        : "Installment offer differs from expected state."
        );
    }
}
