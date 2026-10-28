package com.automation.application.financial.validation.step;

import com.automation.application.financial.validation.FinancialValidationResult;
import com.automation.application.financial.validation.context.FinancialValidationContext;

public interface FinancialValidationStep {

    FinancialValidationResult validate(
            FinancialValidationContext context
    );
}
