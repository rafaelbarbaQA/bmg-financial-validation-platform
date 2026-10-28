package com.automation.domain.mass.record;

import com.automation.domain.account.AccountId;
import com.automation.domain.account.model.AccountReference;
import com.automation.domain.billing.model.BillingCycleReference;
import com.automation.domain.invoice.model.InvoiceReference;
import com.automation.domain.mass.criteria.InstallmentRequirement;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public final class FinancialMassRecordTest {

    private AccountReference account;
    private InvoiceReference invoice;
    private BillingCycleReference billingCycle;

    @BeforeMethod
    public void setUp() {
        account = new AccountReference(
                new AccountId("2310664"),
                596,
                0
        );

        invoice = new InvoiceReference(
                "466031434",
                "123456789012345",
                new BigDecimal("1000.00"),
                new BigDecimal("200.00")
        );

        billingCycle = new BillingCycleReference(
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 20)
        );
    }

    @Test
    public void shouldCreateFinancialMassRecord() {
        FinancialMassRecord record =
                new FinancialMassRecord(
                        account,
                        invoice,
                        billingCycle,
                        true
                );

        assertSame(record.getAccount(), account);
        assertSame(record.getInvoice(), invoice);
        assertSame(record.getBillingCycle(), billingCycle);
        assertTrue(record.hasInstallment());
    }

    @Test
    public void shouldMatchRequiredInstallment() {
        FinancialMassRecord record =
                createRecordWithInstallment(true);

        assertTrue(
                record.matches(
                        InstallmentRequirement.REQUIRED
                )
        );

        assertFalse(
                record.matches(
                        InstallmentRequirement.FORBIDDEN
                )
        );

        assertTrue(
                record.matches(
                        InstallmentRequirement.ANY
                )
        );
    }

    @Test
    public void shouldMatchForbiddenInstallment() {
        FinancialMassRecord record =
                createRecordWithInstallment(false);

        assertFalse(
                record.matches(
                        InstallmentRequirement.REQUIRED
                )
        );

        assertTrue(
                record.matches(
                        InstallmentRequirement.FORBIDDEN
                )
        );

        assertTrue(
                record.matches(
                        InstallmentRequirement.ANY
                )
        );
    }

    @Test
    public void shouldRejectNullAccountReference() {
        expectThrows(
                NullPointerException.class,
                () -> new FinancialMassRecord(
                        null,
                        invoice,
                        billingCycle,
                        false
                )
        );
    }

    @Test
    public void shouldRejectNullInvoiceReference() {
        expectThrows(
                NullPointerException.class,
                () -> new FinancialMassRecord(
                        account,
                        null,
                        billingCycle,
                        false
                )
        );
    }

    @Test
    public void shouldRejectNullBillingCycleReference() {
        expectThrows(
                NullPointerException.class,
                () -> new FinancialMassRecord(
                        account,
                        invoice,
                        null,
                        false
                )
        );
    }

    @Test
    public void shouldRejectNullInstallmentRequirement() {
        FinancialMassRecord record =
                createRecordWithInstallment(true);

        expectThrows(
                NullPointerException.class,
                () -> record.matches(null)
        );
    }

    private FinancialMassRecord createRecordWithInstallment(
            boolean hasInstallment
    ) {
        return new FinancialMassRecord(
                account,
                invoice,
                billingCycle,
                hasInstallment
        );
    }
}