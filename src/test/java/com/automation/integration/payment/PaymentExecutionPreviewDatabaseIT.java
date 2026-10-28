package com.automation.integration.payment;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.payment.execution.PaymentExecutionCoordinator;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.preparation.PaymentPreparation;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.calculator.PaymentAmountCalculator;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.payment.execution.PreviewPaymentExecutor;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class PaymentExecutionPreviewDatabaseIT {

    @Test
    public void shouldPrepareAndExecutePaymentInPreviewMode() {

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

        PaymentExecutionCoordinator coordinator =
                new PaymentExecutionCoordinator(
                        new PreviewPaymentExecutor()
                );

        PaymentExecutionResult result =
                coordinator.execute(
                        instruction
                );

        System.out.println();
        System.out.println(
                "============================================================"
        );

        System.out.println(
                " PAYMENT EXECUTION PIPELINE - PREVIEW"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Conta....................: "
                        + instruction.getAccountId().getValue()
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
                "Valor pagamento..........: "
                        + instruction.getAmount()
        );


        System.out.println(
                "Execution mode...........: PREVIEW"
        );

        System.out.println(
                "Execution status.........: "
                        + result.getStatus()
        );

        System.out.println(
                "External ID..............: "
                        + result.getExternalPaymentId()
        );

        System.out.println(
                "Mensagem.................: "
                        + result.getMessage()
        );

        System.out.println(
                "REAL PAYMENT EXECUTED....: NO"
        );

        System.out.println(
                "============================================================"
        );
assertTrue(
                result.isSuccessful(),
                "Preview execution should complete successfully."
        );

        assertNotNull(
                result.getExternalPaymentId()
        );

        assertTrue(
                result.getExternalPaymentId()
                        .startsWith(
                                "PREVIEW-"
                        )
        );

        assertEquals(
                result.getInstruction(),
                instruction
        );

        validateExpectedAmount(
                instruction
        );
    }

    private static void validateExpectedAmount(
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
                "Unexpected prepared payment amount."
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