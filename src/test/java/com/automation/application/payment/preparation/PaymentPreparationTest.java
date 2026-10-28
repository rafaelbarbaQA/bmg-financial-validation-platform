package com.automation.application.payment.preparation;

import com.automation.domain.account.AccountId;
import com.automation.domain.account.model.AccountReference;
import com.automation.domain.billing.model.BillingCycleReference;
import com.automation.domain.invoice.model.InvoiceReference;
import com.automation.domain.mass.record.FinancialMassRecord;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.calculator.PaymentAmountCalculator;
import com.automation.domain.payment.instruction.PaymentInstruction;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class PaymentPreparationTest {

    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-08-04T21:00:00Z");

    private PaymentPreparation preparation;
    private FinancialMassRecord massRecord;

    @BeforeMethod
    public void setUp() {
        Clock fixedClock = Clock.fixed(
                FIXED_INSTANT,
                ZoneOffset.UTC
        );

        preparation = new PaymentPreparation(
                new PaymentAmountCalculator(),
                fixedClock
        );

        massRecord = createMassRecord(
                "2310664",
                "466031434",
                "123456789012345",
                "1000.00",
                "200.00"
        );
    }

    @Test
    public void shouldPrepareMinimumPayment() {
        PaymentInstruction instruction =
                preparation.prepare(
                        massRecord,
                        PaymentScenario.MINIMUM
                );

        assertEquals(
                instruction.getAccountId(),
                new AccountId("2310664")
        );

        assertEquals(
                instruction.getScenario(),
                PaymentScenario.MINIMUM
        );

        assertEquals(
                instruction.getAmount(),
                new BigDecimal("200.00")
        );

        assertEquals(
                instruction.getPreparedAt(),
                FIXED_INSTANT
        );
}

    @Test
    public void shouldPreparePaymentBelowMinimum() {
        PaymentInstruction instruction =
                preparation.prepare(
                        massRecord,
                        PaymentScenario.BELOW_MINIMUM
                );

        assertEquals(
                instruction.getAmount(),
                new BigDecimal("100.00")
        );
    }

    @Test
    public void shouldPrepareTwentyPercentPartialPayment() {
        PaymentInstruction instruction =
                preparation.prepare(
                        massRecord,
                        PaymentScenario
                                .BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT
                );

        assertEquals(
                instruction.getAmount(),
                new BigDecimal("360.00")
        );
    }

    @Test
    public void shouldPrepareSixtyPercentPartialPayment() {
        PaymentInstruction instruction =
                preparation.prepare(
                        massRecord,
                        PaymentScenario
                                .BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT
                );

        assertEquals(
                instruction.getAmount(),
                new BigDecimal("680.00")
        );
    }

    @Test
    public void shouldPreparePaymentsForMultipleAccounts() {
        FinancialMassRecord secondRecord = createMassRecord(
                "2310665",
                "466031435",
                "123456789012346",
                "500.00",
                "100.00"
        );

        List<PaymentInstruction> instructions =
                preparation.prepareAll(
                        Arrays.asList(
                                massRecord,
                                secondRecord
                        ),
                        PaymentScenario.MINIMUM
                );

        assertEquals(instructions.size(), 2);

        assertEquals(
                instructions.get(0).getAccountId(),
                new AccountId("2310664")
        );

        assertEquals(
                instructions.get(0).getAmount(),
                new BigDecimal("200.00")
        );

        assertEquals(
                instructions.get(1).getAccountId(),
                new AccountId("2310665")
        );

        assertEquals(
                instructions.get(1).getAmount(),
                new BigDecimal("100.00")
        );
    }

    @Test
    public void shouldReturnImmutableInstructionList() {
        List<PaymentInstruction> instructions =
                preparation.prepareAll(
                        Collections.singletonList(massRecord),
                        PaymentScenario.MINIMUM
                );

        expectThrows(
                UnsupportedOperationException.class,
                () -> instructions.add(
                        preparation.prepare(
                                massRecord,
                                PaymentScenario.TOTAL
                        )
                )
        );
    }

    @Test
    public void shouldRejectDuplicatedAccounts() {
        FinancialMassRecord duplicatedRecord = createMassRecord(
                "2310664",
                "466031999",
                "123456789019999",
                "700.00",
                "150.00"
        );

        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> preparation.prepareAll(
                        Arrays.asList(
                                massRecord,
                                duplicatedRecord
                        ),
                        PaymentScenario.MINIMUM
                )
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass record list cannot contain duplicated accounts: 2310664"
        );
    }

    @Test
    public void shouldRejectEmptyMassList() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> preparation.prepareAll(
                        Collections.emptyList(),
                        PaymentScenario.MINIMUM
                )
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass record list cannot be empty."
        );
    }

    @Test
    public void shouldRejectMassListContainingNull() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> preparation.prepareAll(
                        Arrays.asList(
                                massRecord,
                                null
                        ),
                        PaymentScenario.MINIMUM
                )
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass record list cannot contain null values."
        );
    }

    @Test
    public void shouldRejectNullMassRecord() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> preparation.prepare(
                        null,
                        PaymentScenario.MINIMUM
                )
        );

        assertEquals(
                exception.getMessage(),
                "Financial mass record cannot be null."
        );
    }

    @Test
    public void shouldRejectNullScenario() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> preparation.prepare(
                        massRecord,
                        null
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment scenario cannot be null."
        );
    }

    private FinancialMassRecord createMassRecord(
            String accountId,
            String ticketId,
            String nossoNumero,
            String totalAmount,
            String minimumAmount
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
                        nossoNumero,
                        new BigDecimal(totalAmount),
                        new BigDecimal(minimumAmount)
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