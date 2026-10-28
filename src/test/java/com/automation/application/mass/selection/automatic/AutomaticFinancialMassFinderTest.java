package com.automation.application.mass.selection.automatic;

import com.automation.application.mass.repository.FinancialMassRepository;
import com.automation.domain.account.AccountId;
import com.automation.domain.account.model.AccountReference;
import com.automation.domain.billing.model.BillingCycleReference;
import com.automation.domain.invoice.model.InvoiceReference;
import com.automation.domain.mass.FinancialMassSelectionMode;
import com.automation.domain.mass.criteria.FinancialMassCriteria;
import com.automation.domain.mass.criteria.InstallmentRequirement;
import com.automation.domain.mass.record.FinancialMassRecord;
import com.automation.domain.mass.request.FinancialMassRequest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class AutomaticFinancialMassFinderTest {

    @Test
    public void shouldFindRequestedAutomaticFinancialMass() {
        FinancialMassRequest request = createAutomaticRequest(2);

        FinancialMassRepository repository =
                new FakeFinancialMassRepository(
                        Arrays.asList(
                                createMassRecord(
                                        "2310664",
                                        "466031434"
                                ),
                                createMassRecord(
                                        "2310665",
                                        "466031435"
                                ),
                                createMassRecord(
                                        "2310666",
                                        "466031436"
                                )
                        )
                );

        AutomaticFinancialMassFinder finder =
                new AutomaticFinancialMassFinder(
                        request,
                        repository
                );

        List<FinancialMassRecord> result = finder.find();

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
    public void shouldReturnImmutableSelectedMass() {
        FinancialMassRequest request = createAutomaticRequest(1);

        AutomaticFinancialMassFinder finder =
                new AutomaticFinancialMassFinder(
                        request,
                        new FakeFinancialMassRepository(
                                Collections.singletonList(
                                        createMassRecord(
                                                "2310664",
                                                "466031434"
                                        )
                                )
                        )
                );

        List<FinancialMassRecord> result = finder.find();

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
    public void shouldRejectInsufficientFinancialMass() {
        FinancialMassRequest request = createAutomaticRequest(5);

        AutomaticFinancialMassFinder finder =
                new AutomaticFinancialMassFinder(
                        request,
                        new FakeFinancialMassRepository(
                                Arrays.asList(
                                        createMassRecord(
                                                "2310664",
                                                "466031434"
                                        ),
                                        createMassRecord(
                                                "2310665",
                                                "466031435"
                                        )
                                )
                        )
                );

        IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                finder::find
        );

        assertEquals(
                exception.getMessage(),
                "Insufficient eligible financial mass. Requested: 5, found: 2."
        );
    }

    @Test
    public void shouldRejectDuplicatedAccountsFromRepository() {
        FinancialMassRequest request = createAutomaticRequest(2);

        AutomaticFinancialMassFinder finder =
                new AutomaticFinancialMassFinder(
                        request,
                        new FakeFinancialMassRepository(
                                Arrays.asList(
                                        createMassRecord(
                                                "2310664",
                                                "466031434"
                                        ),
                                        createMassRecord(
                                                "2310664",
                                                "466031999"
                                        )
                                )
                        )
                );

        IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                finder::find
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass repository returned duplicated account: 2310664"
        );
    }

    @Test
    public void shouldRejectRepositoryResultContainingNull() {
        FinancialMassRequest request = createAutomaticRequest(2);

        AutomaticFinancialMassFinder finder =
                new AutomaticFinancialMassFinder(
                        request,
                        new FakeFinancialMassRepository(
                                Arrays.asList(
                                        createMassRecord(
                                                "2310664",
                                                "466031434"
                                        ),
                                        null
                                )
                        )
                );

        IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                finder::find
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass repository result cannot contain null values."
        );
    }

    @Test
    public void shouldRejectNullRepositoryResult() {
        FinancialMassRequest request = createAutomaticRequest(1);

        AutomaticFinancialMassFinder finder =
                new AutomaticFinancialMassFinder(
                        request,
                        new NullFinancialMassRepository()
                );

        NullPointerException exception = expectThrows(
                NullPointerException.class,
                finder::find
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass repository result cannot be null."
        );
    }

    @Test
    public void shouldRejectManualRequest() {
        FinancialMassCriteria criteria =
                new FinancialMassCriteria(
                        0,
                        InstallmentRequirement.REQUIRED
                );

        FinancialMassRequest manualRequest =
                new FinancialMassRequest(
                        2,
                        FinancialMassSelectionMode.MANUAL,
                        criteria
                );

        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new AutomaticFinancialMassFinder(
                        manualRequest,
                        new FakeFinancialMassRepository(
                                Collections.emptyList()
                        )
                )
        );

        assertEquals(
                exception.getMessage(),
                "Automatic financial mass finder requires an AUTOMATIC request."
        );
    }

    @Test
    public void shouldRejectNullRepository() {
        FinancialMassRequest request = createAutomaticRequest(1);

        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> new AutomaticFinancialMassFinder(
                        request,
                        null
                )
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass repository cannot be null."
        );
    }

    private FinancialMassRequest createAutomaticRequest(
            int quantity
    ) {
        FinancialMassCriteria criteria =
                FinancialMassCriteria.builder()
                        .accountStatus(0)
                        .productId(596)
                        .withInstallments()
                        .minimumAmountGreaterThan(
                                new BigDecimal("100.00")
                        )
                        .build();

        return new FinancialMassRequest(
                quantity,
                FinancialMassSelectionMode.AUTOMATIC,
                criteria
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
                true
        );
    }

    private static final class FakeFinancialMassRepository
            implements FinancialMassRepository {

        private final List<FinancialMassRecord> records;

        private FakeFinancialMassRepository(
                List<FinancialMassRecord> records
        ) {
            this.records = records;
        }

        @Override
        public List<FinancialMassRecord> findCandidates(
                FinancialMassCriteria criteria,
                int quantity
        ) {
            return records;
        }
    }

    private static final class NullFinancialMassRepository
            implements FinancialMassRepository {

        @Override
        public List<FinancialMassRecord> findCandidates(
                FinancialMassCriteria criteria,
                int quantity
        ) {
            return null;
        }
    }
}