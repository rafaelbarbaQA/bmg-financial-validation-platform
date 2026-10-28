package com.automation.application.mass.selection;

import com.automation.domain.mass.record.FinancialMassRecord;

import java.util.List;

public interface FinancialMassFinder {

    List<FinancialMassRecord> find();
}