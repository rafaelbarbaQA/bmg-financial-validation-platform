package com.automation.application.payment.preparation;

import com.automation.domain.invoice.model.InvoiceReference;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.mass.record.FinancialMassRecord;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.calculator.PaymentAmountCalculator;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.InvoiceSnapshot;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PaymentPreparation {

    private final PaymentAmountCalculator paymentAmountCalculator;
    private final Clock clock;

    public PaymentPreparation(
            PaymentAmountCalculator paymentAmountCalculator,
            Clock clock
    ) {
        this.paymentAmountCalculator = Objects.requireNonNull(
                paymentAmountCalculator,
                "Payment amount calculator cannot be null."
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock cannot be null."
        );
    }

    public PaymentPreparation(
            PaymentAmountCalculator paymentAmountCalculator
    ) {
        this(
                paymentAmountCalculator,
                Clock.systemUTC()
        );
    }

    public PaymentInstruction prepare(
            FinancialMassRecord massRecord,
            PaymentScenario scenario
    ) {
        Objects.requireNonNull(
                massRecord,
                "Financial mass record cannot be null."
        );

        Objects.requireNonNull(
                scenario,
                "Payment scenario cannot be null."
        );

        InvoiceSnapshot invoiceSnapshot =
                createInvoiceSnapshot(massRecord);

        BigDecimal paymentAmount =
                paymentAmountCalculator.calculate(
                        scenario,
                        invoiceSnapshot
                );

        Instant preparedAt = clock.instant();

        return new PaymentInstruction(
                massRecord.getAccount().getAccountId(),
                scenario,
                paymentAmount,
                preparedAt
        );
    }


    public PaymentInstruction prepareFromSnapshot(
            FinancialSnapshot financialSnapshot,
            PaymentScenario scenario
    ) {
        Objects.requireNonNull(
                financialSnapshot,
                "Financial snapshot cannot be null."
        );

        Objects.requireNonNull(
                scenario,
                "Payment scenario cannot be null."
        );

        InvoiceSnapshot invoiceSnapshot =
                new InvoiceSnapshot(
                        financialSnapshot
                                .getAccountId()
                                .getValue(),
                        financialSnapshot
                                .getTicketId(),
                        financialSnapshot
                                .getInvoiceAmount(),
                        financialSnapshot
                                .getMinimumAmount()
                );

        BigDecimal paymentAmount =
                paymentAmountCalculator.calculate(
                        scenario,
                        invoiceSnapshot
                );

        return new PaymentInstruction(
                financialSnapshot.getAccountId(),
                scenario,
                paymentAmount,
                clock.instant()
        );
    }
    public List<PaymentInstruction> prepareAll(
            List<FinancialMassRecord> massRecords,
            PaymentScenario scenario
    ) {
        List<FinancialMassRecord> validatedRecords =
                requireValidMassRecords(massRecords);

        Objects.requireNonNull(
                scenario,
                "Payment scenario cannot be null."
        );

        validateDuplicatedAccounts(validatedRecords);

        List<PaymentInstruction> instructions =
                new ArrayList<PaymentInstruction>();

        for (FinancialMassRecord massRecord : validatedRecords) {
            instructions.add(
                    prepare(
                            massRecord,
                            scenario
                    )
            );
        }

        return Collections.unmodifiableList(instructions);
    }

    private InvoiceSnapshot createInvoiceSnapshot(
            FinancialMassRecord massRecord
    ) {
        InvoiceReference invoice =
                massRecord.getInvoice();

        return new InvoiceSnapshot(
                massRecord.getAccount()
                        .getAccountId()
                        .getValue(),
                invoice.getTicketId(),
                invoice.getTotalAmount(),
                invoice.getMinimumAmount()
        );
    }

    private static List<FinancialMassRecord> requireValidMassRecords(
            List<FinancialMassRecord> massRecords
    ) {
        Objects.requireNonNull(
                massRecords,
                "Financial mass record list cannot be null."
        );

        if (massRecords.isEmpty()) {
            throw new IllegalArgumentException(
                    "Financial mass record list cannot be empty."
            );
        }

        List<FinancialMassRecord> copiedRecords =
                new ArrayList<FinancialMassRecord>(massRecords);

        if (copiedRecords.contains(null)) {
            throw new IllegalArgumentException(
                    "Financial mass record list cannot contain null values."
            );
        }

        return copiedRecords;
    }

    private static void validateDuplicatedAccounts(
            List<FinancialMassRecord> massRecords
    ) {
        Set<String> accountIds = new HashSet<String>();

        for (FinancialMassRecord massRecord : massRecords) {
            String accountId = massRecord
                    .getAccount()
                    .getAccountId()
                    .getValue();

            if (!accountIds.add(accountId)) {
                throw new IllegalArgumentException(
                        "Financial mass record list cannot contain duplicated accounts: "
                                + accountId
                );
            }
        }
    }
}