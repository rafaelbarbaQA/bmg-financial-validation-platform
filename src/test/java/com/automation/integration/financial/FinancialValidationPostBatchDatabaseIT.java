package com.automation.integration.financial;

import com.automation.domain.financial.scenario.FullPaymentScenario;
import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.financial.validation.FinancialValidationReport;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.engine.FinancialValidationEngine;
import com.automation.application.financial.validation.engine.FinancialValidationEngineFactory;
import com.automation.application.payment.processing.PaymentProcessingStateService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.model.PaymentProcessingState;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.database.repository.SqlServerPaymentProcessingStateRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class FinancialValidationPostBatchDatabaseIT {

    @Test
    public void shouldValidateRealPostBatchFinancialState() {

        String accountIdValue =
                requireProperty(
                        "validation.account.id"
                );

        long paymentEventId =
                Long.parseLong(
                        requireProperty(
                                "payment.event.id"
                        )
                );

        int expectedAccountStatus =
                Integer.parseInt(
                        System.getProperty(
                                "expected.account.status",
                                "0"
                        )
                );

        AccountId accountId =
                new AccountId(
                        accountIdValue
                );

        // -------------------------------------------------
        // 1. REAL FINANCIAL SNAPSHOT
        // -------------------------------------------------

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot afterSnapshot =
                snapshotService.capture(
                        accountId
                );

        // -------------------------------------------------
        // 2. REAL PAYMENT PROCESSING STATE
        // -------------------------------------------------

        PaymentInstruction instruction =
                createDummyInstruction(
                        accountId
                );

        PaymentExecutionResult executionResult =
                PaymentExecutionResult.success(
                        instruction,
                        String.valueOf(
                                paymentEventId
                        ),
                        "Existing payment event used by post-batch financial validation.",
                        Instant.now()
                );

        PaymentProcessingStateService processingStateService =
                new PaymentProcessingStateService(
                        new SqlServerPaymentProcessingStateRepository()
                );

        PaymentProcessingState processingState =
                processingStateService.load(
                        executionResult
                );

        // -------------------------------------------------
        // 3. VALIDATION CONTEXT
        //
        // Full-payment pipeline currently validates:
        // PAYMENT_STATUS + ACCOUNT_STATUS.
        //
        // No balance rule consumes the before snapshot.
        // Until historical before-snapshot persistence exists,
        // the real current snapshot is reused as context baseline.
        // -------------------------------------------------

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        afterSnapshot,
                        afterSnapshot,
                        afterSnapshot.getInvoiceAmount(),
                        processingState
                );

        // -------------------------------------------------
        // 4. REAL VALIDATION ENGINE
        // -------------------------------------------------

        FinancialValidationEngine engine =
                FinancialValidationEngineFactory.forFullPayment(
                        expectedAccountStatus
                );

        FinancialValidationReport report =
                engine.validate(
                        context
                );

        // -------------------------------------------------
        // 5. OUTPUT
        // -------------------------------------------------

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " FINANCIAL VALIDATION - REAL POST-BATCH DATABASE"
        );
        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Conta....................: "
                        + afterSnapshot.getAccountId().getValue()
        );

        System.out.println(
                "Evento pagamento.........: "
                        + paymentEventId
        );

        System.out.println(
                "Status pagamento.........: "
                        + processingState.getStatus()
        );

        System.out.println(
                "Status conta atual.......: "
                        + afterSnapshot.getAccountStatus()
        );

        System.out.println(
                "Status conta esperado....: "
                        + expectedAccountStatus
        );

        System.out.println(
                "Saldo atual..............: "
                        + afterSnapshot.getCurrentBalance()
        );

        System.out.println(
                "Valor minimo.............: "
                        + afterSnapshot.getMinimumAmount()
        );

        System.out.println(
                "Valor fatura.............: "
                        + afterSnapshot.getInvoiceAmount()
        );

        System.out.println(
                "NossoNumero..............: "
                        + afterSnapshot.getNossoNumero()
        );

        System.out.println(
                "Vencimento...............: "
                        + afterSnapshot.getDueDate()
        );

        System.out.println(
                "Validacoes executadas....: "
                        + report.getResults().size()
        );

        report.getResults().forEach(
                result -> System.out.println(
                        result.getRule()
                                + " -> "
                                + result.getStatus()
                                + " | esperado="
                                + result.getExpected()
                                + " | atual="
                                + result.getActual()
                                + " | "
                                + result.getMessage()
                )
        );

        System.out.println(
                "Resultado final..........: "
                        + (report.passed() ? "PASS" : "FAIL")
        );

        System.out.println(
                "============================================================"
        );

        // -------------------------------------------------
        // 6. ASSERTIONS
        // -------------------------------------------------

        assertEquals(
                processingState.getStatus(),
                2,
                "Payment must be processed."
        );

        assertEquals(
                afterSnapshot.getAccountStatus(),
                expectedAccountStatus,
                "Account status does not match expected value."
        );

        assertTrue(
                report.passed(),
                "Financial validation report failed."
        );
    }

    private static PaymentInstruction createDummyInstruction(
            AccountId accountId
    ) {

        return new PaymentInstruction(
                accountId,
                PaymentScenario.TOTAL,
                BigDecimal.ONE,
                Instant.now()
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