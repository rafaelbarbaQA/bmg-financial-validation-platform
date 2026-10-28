package com.automation.application.mass.repository;

import com.automation.domain.mass.criteria.FinancialMassCriteria;
import com.automation.domain.mass.record.FinancialMassRecord;

import java.util.List;

public interface FinancialMassRepository {

    List<FinancialMassRecord> findCandidates(
            FinancialMassCriteria criteria,
            int quantity
    );
}