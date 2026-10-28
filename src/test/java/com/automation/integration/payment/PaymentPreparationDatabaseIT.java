package com.automation.integration.payment;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.payment.preparation.PaymentPreparation;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.calculator.PaymentAmountCalculator;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class PaymentPreparationDatabaseIT {

    @Test
    public void shouldPreparePaymentInstructionUsingRealFinancialData() {

        AccountId accountId =
                new AccountId(
                        requireProperty(
                                "payment.account.id"
                        )
                );

        PaymentScenario scenario =
                PaymentScenario.valueOf(
                        requireProperty(
                                "payment.scenario"
                        )
                        .trim()
                        .toUpperCase()
                );

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot snapshot =
                snapshotService.capture(
                        accountId
                );

        PaymentPreparation preparation =
                new PaymentPreparation(
                        new PaymentAmountCalculator()
                );

        PaymentInstruction instruction =
                preparation.prepareFromSnapshot(
                        snapshot,
                        scenario
                );

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " PAYMENT PREPARATION - REAL DATABASE"
        );
        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Conta....................: "
                        + snapshot.getAccountId().getValue()
        );

        System.out.println(
                "Cenario..................: "
                        + instruction.getScenario()
        );

        System.out.println(
                "Boleto...................: "
                        + snapshot.getTicketId()
        );

        System.out.println(
                "NossoNumero..............: "
                        + snapshot.getNossoNumero()
        );

        System.out.println(
                "Saldo fatura.............: "
                        + snapshot.getInvoiceAmount()
        );

        System.out.println(
                "Valor minimo.............: "
                        + snapshot.getMinimumAmount()
        );

        System.out.println(
                "Saldo atual..............: "
                        + snapshot.getCurrentBalance()
        );

        System.out.println(
                "Valor pagamento..........: "
                        + instruction.getAmount()
        );

        System.out.println(
                "Preparado em.............: "
                        + instruction.getPreparedAt()
        );


        System.out.println(
                "============================================================"
        );

        assertEquals(
                instruction.getAccountId(),
                accountId
        );

        assertEquals(
                instruction.getScenario(),
                scenario
        );
assertTrue(
                instruction.getAmount()
                        .compareTo(BigDecimal.ZERO) > 0,
                "Payment amount must be greater than zero."
        );

        validateExpectedAmountIfProvided(
                instruction
        );
    }

    private static void validateExpectedAmountIfProvided(
            PaymentInstruction instruction
    ) {

        String expected =
                System.getProperty(
                        "expected.payment.amount"
                );

        if (expected == null
                || expected.trim().isEmpty()) {
            return;
        }

        BigDecimal expectedAmount =
                new BigDecimal(
                        expected.trim()
                );

        assertEquals(
                instruction.getAmount()
                        .compareTo(expectedAmount),
                0,
                "Calculated payment amount does not match expected value."
        );
    }

    private static String requireProperty(
            String name
    ) {

        String value =
                System.getProperty(
                        name
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + name
                            + "=..."
            );
        }

        return value.trim();
    }
}