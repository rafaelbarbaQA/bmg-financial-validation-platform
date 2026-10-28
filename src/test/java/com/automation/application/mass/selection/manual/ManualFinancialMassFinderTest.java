package com.automation.application.mass.selection.manual;

import com.automation.domain.account.AccountId;
import com.automation.domain.account.model.AccountReference;
import com.automation.domain.billing.model.BillingCycleReference;
import com.automation.domain.invoice.model.InvoiceReference;
import com.automation.domain.mass.record.FinancialMassRecord;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class ManualFinancialMassFinderTest {

    @Test
    public void shouldReturnManualFinancialMass() {
        FinancialMassRecord firstRecord =
                createMassRecord(
                        "2310664",
                        "466031434"
                );

        FinancialMassRecord secondRecord =
                createMassRecord(
                        "2310665",
                        "466031435"
                );

        ManualFinancialMassFinder finder =
                new ManualFinancialMassFinder(
                        Arrays.asList(
                                firstRecord,
                                secondRecord
                        )
                );

        List<FinancialMassRecord> result =
                finder.find();

        assertEquals(result.size(), 2);

        assertEquals(
                result.get(0)
                        .getAccount()
                        .getAccountId(),
                new AccountId("2310664")
        );

        assertEquals(
                result.get(1)
                        .getAccount()
                        .getAccountId(),
                new AccountId("2310665")
        );
    }

    @Test
    public void shouldReturnImmutableMassList() {
        ManualFinancialMassFinder finder =
                new ManualFinancialMassFinder(
                        Collections.singletonList(
                                createMassRecord(
                                        "2310664",
                                        "466031434"
                                )
                        )
                );

        List<FinancialMassRecord> result =
                finder.find();

        expectThrows(
                UnsupportedOperationException.class,
                () -> result.add(
                        createMassRecord(
                                "2310665",
                                "466031435"
                        )
                )
        );
    }

    @Test
    public void shouldRejectEmptyManualMass() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new ManualFinancialMassFinder(
                        Collections.emptyList()
                )
        );

        assertEquals(
                exception.getMessage(),
                "Manual financial mass list cannot be empty."
        );
    }

    @Test
    public void shouldRejectDuplicatedAccounts() {
        FinancialMassRecord firstRecord =
                createMassRecord(
                        "2310664",
                        "466031434"
                );

        FinancialMassRecord duplicatedRecord =
                createMassRecord(
                        "2310664",
                        "466031999"
                );

        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new ManualFinancialMassFinder(
                        Arrays.asList(
                                firstRecord,
                                duplicatedRecord
                        )
                )
        );

        assertEquals(
                exception.getMessage(),
                "Manual financial mass list cannot contain duplicated accounts: 2310664"
        );
    }

    private FinancialMassRecord createMassRecord(
            String accountId,
            String ticketId
    ) {
        AccountReference account =
                new AccountReference(
                        new AccountId(accountId),
                        596,
                        0
                );

        InvoiceReference invoice =
                new InvoiceReference(
                        ticketId,
                        "123456789012345",
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00")
                );

        BillingCycleReference billingCycle =
                new BillingCycleReference(
                        LocalDate.of(2026, 8, 5),
                        LocalDate.of(2026, 8, 10),
                        LocalDate.of(2026, 8, 20)
                );

        return new FinancialMassRecord(
                account,
                invoice,
                billingCycle,
                false
        );
    }
}