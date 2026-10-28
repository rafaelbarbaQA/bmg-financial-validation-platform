package com.automation.application.financial.validation.validator;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.FinancialValidationStatus;
import com.automation.domain.installment.InstallmentExpectation;

import java.util.Objects;

public final class InstallmentAdhesionValidator {

    public FinancialValidationResult validate(
            InstallmentExpectation expected,
            InstallmentExpectation actual
    ) {
        Objects.requireNonNull(
                expected,
                "Expected installment adhesion cannot be null."
        );

        Objects.requireNonNull(
                actual,
                "Actual installment adhesion cannot be null."
        );

        boolean matches =
                expected == actual;

        return new FinancialValidationResult(
                "INSTALLMENT_ADHESION",
                matches
                        ? FinancialValidationStatus.PASS
                        : FinancialValidationStatus.FAIL,
                expected.name(),
                actual.name(),
                matches
                        ? "Installment adhesion matches expected business rule."
                        : "Installment adhesion differs from expected business rule."
        );
    }
}
