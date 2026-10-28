package com.automation.application.financial.validation.engine;

import com.automation.application.financial.validation.FinancialValidationReport;

public interface ValidationEngine<T> {

    FinancialValidationReport validate(T context);

}