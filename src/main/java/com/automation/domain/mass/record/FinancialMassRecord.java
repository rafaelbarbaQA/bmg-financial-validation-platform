package com.automation.domain.mass.record;

import com.automation.domain.account.model.AccountReference;
import com.automation.domain.billing.model.BillingCycleReference;
import com.automation.domain.invoice.model.InvoiceReference;
import com.automation.domain.mass.criteria.InstallmentRequirement;

import java.util.Objects;

public final class FinancialMassRecord {

    private final AccountReference account;
    private final InvoiceReference invoice;
    private final BillingCycleReference billingCycle;
    private final boolean hasInstallment;

    public FinancialMassRecord(
            AccountReference account,
            InvoiceReference invoice,
            BillingCycleReference billingCycle,
            boolean hasInstallment
    ) {
        this.account = Objects.requireNonNull(
                account,
                "Account reference cannot be null."
        );

        this.invoice = Objects.requireNonNull(
                invoice,
                "Invoice reference cannot be null."
        );

        this.billingCycle = Objects.requireNonNull(
                billingCycle,
                "Billing cycle reference cannot be null."
        );

        this.hasInstallment = hasInstallment;
    }

    public AccountReference getAccount() {
        return account;
    }

    public InvoiceReference getInvoice() {
        return invoice;
    }

    public BillingCycleReference getBillingCycle() {
        return billingCycle;
    }

    public boolean hasInstallment() {
        return hasInstallment;
    }

    public boolean matches(
            InstallmentRequirement requirement
    ) {
        Objects.requireNonNull(
                requirement,
                "Installment requirement cannot be null."
        );

        switch (requirement) {
            case REQUIRED:
                return hasInstallment;

            case FORBIDDEN:
                return !hasInstallment;

            case ANY:
                return true;

            default:
                throw new IllegalArgumentException(
                        "Unsupported installment requirement: " + requirement
                );
        }
    }
}