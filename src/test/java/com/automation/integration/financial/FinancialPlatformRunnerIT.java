package com.automation.integration.financial;
import com.automation.application.financial.prebatch.FinancialPreBatchPurchaseService;
import com.automation.application.purchase.execution.PurchaseExecutionCoordinator;
import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.execution.PurchaseExecutor;
import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import com.automation.infrastructure.purchase.execution.PreviewPurchaseExecutor;
import com.automation.infrastructure.purchase.execution.SqlServerPurchaseExecutor;


import com.automation.core.report.AllureAttachment;
import com.automation.application.financial.runner.FinancialScenarioRunner;
import com.automation.application.financial.runner.PaymentScenarioExecution;
import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.payment.execution.PaymentExecutionCoordinator;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.execution.PaymentExecutor;
import com.automation.application.payment.scenario.FinancialScenarioPaymentMapper;
import com.automation.application.payment.validation.PaymentValidationPlan;
import com.automation.application.payment.validation.PaymentValidationPlanFactory;
import com.automation.application.payment.validation.PaymentEventValidationService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.PaymentEventDetails;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.database.repository.SqlServerPaymentEventDetailsRepository;
import com.automation.infrastructure.financial.snapshot.FinancialSnapshotFileStore;
import com.automation.infrastructure.payment.execution.PreviewPaymentExecutor;
import com.automation.infrastructure.payment.execution.RealPaymentExecutor;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.testng.annotations.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
@Epic("Financial Validation Platform")
@Feature("Pagamentos")
public final class FinancialPlatformRunnerIT {

    private static final String MODE_PREVIEW =
            "PREVIEW";

    private static final String MODE_REAL =
            "REAL";

    @Test
@Story("Execucao financeira")
    public void runFinancialScenario() {

        String accountValue =
                requireProperty(
                        "financial.account"
                );

        boolean paymentEnabled =
                Boolean.parseBoolean(
                        System.getProperty(
                                "financial.payment.enabled",
                                "true"
                        )
                );

        String scenarioValue =
                paymentEnabled
                        ? requireProperty(
                                "financial.scenario"
                        )
                                .trim()
                                .toUpperCase()
                        : "DISABLED";

        String mode =
                obterModoExecucao();

        Allure.parameter(
                "Conta",
                accountValue
        );

        Allure.parameter(
                "Cenario",
                scenarioValue
        );

        Allure.parameter(
                "Modo de execucao",
                mode
        );

        AccountId accountId =
                new AccountId(
                        accountValue
                );

        boolean purchaseEnabled =
                Boolean.parseBoolean(
                        System.getProperty(
                                "financial.purchase.enabled",
                                "false"
                        )
                );

        PurchaseExecutionResult preBatchPurchaseResult =
                null;

        if (purchaseEnabled) {

            int purchaseOperationId =
                    Integer.parseInt(
                            requireProperty(
                                    "financial.purchase.operation"
                            )
                    );

            long purchaseSeed =
                    Long.parseLong(
                            System.getProperty(
                                    "financial.purchase.seed",
                                    "20260723"
                            )
                    );
            LocalDate purchaseDate =
                    LocalDate.parse(
                            requireProperty(
                                    "financial.purchase.date"
                            )
                    );

            PurchaseOperation purchaseOperation =
                    PurchaseOperation.fromOperationId(
                            purchaseOperationId
                    );

            PurchaseExecutor purchaseExecutor =
                    MODE_REAL.equals(mode)
                            ? new SqlServerPurchaseExecutor()
                            : new PreviewPurchaseExecutor();

            FinancialPreBatchPurchaseService purchaseService =
                    new FinancialPreBatchPurchaseService(
                            new PurchaseInstructionGenerator(
                                    purchaseSeed,
                                    purchaseDate
                            ),
                            new PurchaseExecutionCoordinator(
                                    purchaseExecutor
                            )
                    );

            preBatchPurchaseResult =
                    purchaseService.execute(
                            accountId,
                            purchaseOperation
                    );

            PurchaseInstruction purchaseInstruction =
                    preBatchPurchaseResult.getInstruction();

            Allure.parameter(
                    "PRE-BATCH purchase",
                    "ENABLED"
            );

            Allure.parameter(
                    "Purchase operation",
                    purchaseOperation.getOperationId()
            );

            Allure.parameter(
                    "Purchase amount",
                    purchaseInstruction.getPurchaseAmount()
            );

            Allure.parameter(
                    "Purchase installments",
                    purchaseInstruction.getInstallmentCount()
            );

            Allure.parameter(
                    "Purchase installment amount",
                    purchaseInstruction.getInstallmentAmount()
            );

            System.out.println();
            System.out.println(
                    "================ PRE-BATCH PURCHASE ================"
            );

            System.out.println(
                    "Conta....................: "
                            + accountId.getValue()
            );

            System.out.println(
                    "IdOperacao...............: "
                            + purchaseOperation.getOperationId()
            );

            System.out.println(
                    "Valor compra.............: "
                            + purchaseInstruction.getPurchaseAmount()
            );

            System.out.println(
                    "Parcelas.................: "
                            + purchaseInstruction.getInstallmentCount()
            );

            System.out.println(
                    "Valor parcela............: "
                            + purchaseInstruction.getInstallmentAmount()
            );

            System.out.println(
                    "Valor contrato...........: "
                            + purchaseInstruction.getContractAmount()
            );

            System.out.println(
                    "Data compra..............: "
                            + purchaseInstruction.getPurchaseDate()
            );

            System.out.println(
                    "Data entrada.............: "
                            + purchaseInstruction.getEntryDate()
            );

            System.out.println(
                    "Modo.....................: "
                            + mode
            );

            System.out.println(
                    "Banco alterado...........: "
                            + (MODE_REAL.equals(mode)
                            ? "SIM"
                            : "NAO")
            );

            System.out.println(
                    "===================================================="
            );
            System.out.println();
        } else {

            Allure.parameter(
                    "PRE-BATCH purchase",
                    "DISABLED"
            );
        }

        Allure.parameter(
                "PRE-BATCH payment",
                paymentEnabled
                        ? "ENABLED"
                        : "DISABLED"
        );

        if (!paymentEnabled) {

            System.out.println();
            System.out.println(
                    "============================================================"
            );
            System.out.println(
                    " FINANCIAL PLATFORM - PRE-BATCH"
            );
            System.out.println(
                    "============================================================"
            );

            System.out.println(
                    "Conta....................: "
                            + accountId.getValue()
            );

            System.out.println(
                    "Compra PRE-BATCH.........: "
                            + (purchaseEnabled
                            ? "ENABLED"
                            : "DISABLED")
            );

            System.out.println(
                    "Pagamento PRE-BATCH......: DISABLED"
            );

            System.out.println(
                    "Modo.....................: "
                            + mode
            );

            if (MODE_PREVIEW.equals(mode)) {

                Allure.parameter(
                        "BEFORE snapshot",
                        "NOT CAPTURED IN PREVIEW"
                );

                System.out.println(
                        "Snapshot BEFORE..........: NAO CAPTURADO"
                );

                System.out.println(
                        "Banco acessado...........: NAO"
                );

                System.out.println(
                        "Resultado................: PREVIEW CONCLUIDO"
                );

                System.out.println(
                        "============================================================"
                );

                return;
            }

            FinancialSnapshotService preBatchSnapshotService =
                    new FinancialSnapshotService(
                            new SqlServerFinancialSnapshotRepository()
                    );

            FinancialSnapshot preBatchSnapshot =
                    preBatchSnapshotService.capture(
                            accountId
                    );

            Allure.parameter(
                    "BEFORE current balance",
                    preBatchSnapshot.getCurrentBalance()
            );

            Allure.parameter(
                    "BEFORE invoice amount",
                    preBatchSnapshot.getInvoiceAmount()
            );

            System.out.println(
                    "Saldo BEFORE.............: "
                            + preBatchSnapshot.getCurrentBalance()
            );

            System.out.println(
                    "Saldo fatura BEFORE......: "
                            + preBatchSnapshot.getInvoiceAmount()
            );

            System.out.println(
                    "Snapshot BEFORE..........: CAPTURADO"
            );

            System.out.println(
                    "============================================================"
            );

            return;
        }
        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialScenarioPaymentMapper mapper =
                new FinancialScenarioPaymentMapper();

        PaymentValidationPlanFactory planFactory =
                new PaymentValidationPlanFactory();

        PaymentScenarioExecution execution =
                createExecution(
                        accountId,
                        scenarioValue
                );

        /*
         * Primeiro montamos todo o contexto financeiro.
         *
         * Nenhuma movimentação real ocorre nesta etapa.
         */
        execution
                .captureBefore(
                        snapshotService
                )
                .preparePayment(
                        mapper,
                        Clock.systemUTC()
                )
                .createValidationPlan(
                        planFactory
                );

        FinancialSnapshot snapshot =
                execution.getBeforeSnapshot();
        if (MODE_REAL.equals(mode)) {

            FinancialSnapshotFileStore snapshotStore =
                    new FinancialSnapshotFileStore();

            String beforeSnapshotPath =
                    snapshotStore
                            .saveBefore(snapshot)
                            .toAbsolutePath()
                            .toString();

            Allure.parameter(
                    "BEFORE snapshot",
                    beforeSnapshotPath
            );

            System.out.println(
                    "BEFORE snapshot persistido: "
                            + beforeSnapshotPath
            );
        }

        /*
         * Somente depois do snapshot e da preparação
         * escolhemos o executor.
         *
         * PREVIEW -> não altera banco.
         * REAL    -> lançamento real protegido.
         */
        PaymentExecutor paymentExecutor =
                createPaymentExecutor(
                        mode,
                        snapshot
                );

        PaymentExecutionCoordinator coordinator =
                new PaymentExecutionCoordinator(
                        paymentExecutor
                );

        execution.executePayment(
                coordinator
        );

        PaymentInstruction instruction =
                execution.getPaymentInstruction();

        PaymentValidationPlan validationPlan =
                execution.getValidationPlan();

        
PaymentExecutionResult executionResult =
                execution.getPaymentExecutionResult();

        PaymentEventDetails paymentEventDetails = null;

        if (MODE_REAL.equals(mode)) {

            int expectedInitialStatus =
                    getExpectedInitialPaymentStatus();

            PaymentEventValidationService databaseValidation =
                    new PaymentEventValidationService(
                            new SqlServerPaymentEventDetailsRepository()
                    );

            paymentEventDetails =
                    databaseValidation.validateInsertedPayment(
                            executionResult,
                            snapshot.getNossoNumero(),
                            expectedInitialStatus
                    );

            Allure.parameter(
                    "Status inicial esperado",
                    expectedInitialStatus
            );

            Allure.parameter(
                    "Status inicial encontrado",
                    paymentEventDetails.getStatus()
            );

            imprimirValidacaoBanco(
                    paymentEventDetails
            );
        }

        imprimirResumo(
                mode,
                accountId,
                snapshot,
                instruction,
                validationPlan,
                executionResult
        );
        anexarResumoExecutivoPagamento(
                mode,
                accountId,
                snapshot,
                instruction,
                validationPlan,
                executionResult,
                paymentEventDetails
        );

        assertNotNull(
                snapshot
        );

        assertNotNull(
                instruction
        );

        assertNotNull(
                validationPlan
        );

        assertNotNull(
                executionResult
        );

        assertTrue(
                executionResult.isSuccessful()
        );
    }

    private static PaymentExecutor createPaymentExecutor(
            String mode,
            FinancialSnapshot snapshot
    ) {

        if (MODE_PREVIEW.equals(mode)) {

            return new PreviewPaymentExecutor();
        }

        if (MODE_REAL.equals(mode)) {

            LocalDate paymentDate =
                    requireLocalDateProperty(
                            "financial.paymentDate"
                    );

            Allure.parameter(
                    "Data pagamento",
                    paymentDate
            );

            Allure.parameter(
                    "IdBoleto",
                    snapshot.getTicketId()
            );

            long idBoleto;

            try {

                idBoleto =
                        Long.parseLong(
                                snapshot.getTicketId()
                        );

            } catch (NumberFormatException exception) {

                throw new IllegalStateException(
                        "O IdBoleto retornado pelo snapshot não é numérico: "
                                + snapshot.getTicketId(),
                        exception
                );
            }

            return new RealPaymentExecutor(
                    idBoleto,
                    paymentDate
            );
        }

        throw new IllegalArgumentException(
                "Modo de execucao nao suportado: "
                        + mode
                        + ". Utilize PREVIEW ou REAL."
        );
    }

    private static String obterModoExecucao() {

        String mode =
                System.getProperty(
                        "financial.mode",
                        MODE_PREVIEW
                )
                        .trim()
                        .toUpperCase();

        if (!MODE_PREVIEW.equals(mode)
                && !MODE_REAL.equals(mode)) {

            throw new IllegalArgumentException(
                    "Modo de execucao invalido: "
                            + mode
                            + ". Utilize PREVIEW ou REAL."
            );
        }

        return mode;
    }

    private static LocalDate requireLocalDateProperty(
            String property
    ) {

        String value =
                requireProperty(
                        property
                );

        try {

            return LocalDate.parse(
                    value
            );

        } catch (DateTimeParseException exception) {

            throw new IllegalArgumentException(
                    "A propriedade -D"
                            + property
                            + " deve estar no formato AAAA-MM-DD. "
                            + "Valor recebido: "
                            + value,
                    exception
            );
        }
    }

    private static void imprimirResumo(
            String mode,
            AccountId accountId,
            FinancialSnapshot snapshot,
            PaymentInstruction instruction,
            PaymentValidationPlan validationPlan,
            PaymentExecutionResult executionResult
    ) {

        System.out.println();
        System.out.println(
                "============================================================"
        );

        System.out.println(
                " FINANCIAL VALIDATION PLATFORM"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Conta....................: "
                        + accountId.getValue()
        );

        System.out.println(
                "Cenario..................: "
                        + instruction.getScenario()
        );

        System.out.println(
                "------------------------------------------------------------"
        );

        System.out.println(
                "Saldo atual..............: "
                        + snapshot.getCurrentBalance()
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
                "Boleto...................: "
                        + snapshot.getTicketId()
        );

        System.out.println(
                "NossoNumero..............: "
                        + snapshot.getNossoNumero()
        );

        System.out.println(
                "------------------------------------------------------------"
        );

        System.out.println(
                "Valor pagamento..........: "
                        + instruction.getAmount()
        );


        System.out.println(
                "Parcelamento esperado....: "
                        + validationPlan.getInstallmentExpectation()
        );

        System.out.println(
                "Execution mode...........: "
                        + mode
        );

        System.out.println(
                "Execution status.........: "
                        + executionResult.getStatus()
        );

        System.out.println(
                "External ID..............: "
                        + executionResult.getExternalPaymentId()
        );

        System.out.println(
                "Execution message........: "
                        + executionResult.getMessage()
        );

        System.out.println(
                "------------------------------------------------------------"
        );

        System.out.println(
                "BEFORE SNAPSHOT..........: CAPTURED"
        );

        System.out.println(
                "PAYMENT VALUE............: CALCULATED"
        );

        System.out.println(
                "PAYMENT INSTRUCTION......: READY"
        );

        System.out.println(
                "VALIDATION PLAN..........: CREATED"
        );

        if (MODE_REAL.equals(mode)) {

            System.out.println(
                    "REAL PAYMENT.............: EXECUTED"
            );

        } else {

            System.out.println(
                    "REAL PAYMENT.............: NOT EXECUTED"
            );
        }

        System.out.println(
                "POST-BATCH...............: PENDING PROCESSING"
        );

        System.out.println(
                "============================================================"
        );
    }

    private static void anexarResumoExecutivoPagamento(
            String mode,
            AccountId accountId,
            FinancialSnapshot snapshot,
            PaymentInstruction instruction,
            PaymentValidationPlan validationPlan,
            PaymentExecutionResult executionResult,
            PaymentEventDetails paymentEventDetails
    ) {

        String quebra =
                System.lineSeparator();

        String resultado =
                executionResult.isSuccessful()
                        ? "PASS"
                        : "FAIL";

        String persistenciaSql =
                MODE_REAL.equals(mode)
                        ? "VALIDADA"
                        : "NAO APLICAVEL - PREVIEW";

        String statusProcessamento =
                paymentEventDetails == null
                        ? "NAO APLICAVEL"
                        : String.valueOf(
                                paymentEventDetails.getStatus()
                        );

        String idEventoPagamento =
                paymentEventDetails == null
                        ? "NAO APLICAVEL"
                        : String.valueOf(
                                paymentEventDetails
                                        .getPaymentEventId()
                        );

        String proximaEtapa =
                MODE_REAL.equals(mode)
                        ? "PROCESSAMENTO BATCH / VALIDACAO POS-BATCH"
                        : "EXECUCAO REAL CONTROLADA";

        String resumo =
                "FINANCIAL VALIDATION PLATFORM"
                        + quebra
                        + "========================================"
                        + quebra
                        + "RESUMO EXECUTIVO - PAGAMENTO"
                        + quebra
                        + "========================================"
                        + quebra
                        + quebra
                        + "Resultado................: "
                        + resultado
                        + quebra
                        + "Modo.....................: "
                        + mode
                        + quebra
                        + quebra
                        + "Conta....................: "
                        + accountId.getValue()
                        + quebra
                        + "Cenario..................: "
                        + instruction.getScenario()
                        + quebra
                        + quebra
                        + "Saldo atual..............: "
                        + snapshot.getCurrentBalance()
                        + quebra
                        + "Saldo da fatura..........: "
                        + snapshot.getInvoiceAmount()
                        + quebra
                        + "Valor minimo.............: "
                        + snapshot.getMinimumAmount()
                        + quebra
                        + "Valor do pagamento.......: "
                        + instruction.getAmount()
                        + quebra
                        + quebra
                        + "IdBoleto.................: "
                        + snapshot.getTicketId()
                        + quebra
                        + "NossoNumero..............: "
                        + snapshot.getNossoNumero()
                        + quebra
                        + "IdEventoPagamento........: "
                        + idEventoPagamento
                        + quebra
                        + quebra
                        + "Parcelamento esperado....: "
                        + validationPlan
                                .getInstallmentExpectation()
                        + quebra
                        + "Persistencia SQL.........: "
                        + persistenciaSql
                        + quebra
                        + "Status processamento.....: "
                        + statusProcessamento
                        + quebra
                        + "Proxima etapa............: "
                        + proximaEtapa;

        AllureAttachment
                .anexarResumoExecutivoPagamento(
                        resumo
                );
    }
    private static int getExpectedInitialPaymentStatus() {

        String value =
                System.getProperty(
                        "financial.expectedInitialPaymentStatus",
                        "0"
                );

        try {

            int status =
                    Integer.parseInt(
                            value.trim()
                    );

            if (status < 0) {

                throw new IllegalArgumentException(
                        "Initial payment status cannot be negative."
                );
            }

            return status;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "financial.expectedInitialPaymentStatus "
                            + "must contain a numeric value. Actual: "
                            + value,
                    exception
            );
        }
    }

    private static void imprimirValidacaoBanco(
            PaymentEventDetails details
    ) {

        System.out.println();
        System.out.println(
                "================ DATABASE VALIDATION ================="
        );

        System.out.println(
                "Payment event ID.........: "
                        + details.getPaymentEventId()
        );

        System.out.println(
                "Account..................: "
                        + details.getAccountId().getValue()
        );

        System.out.println(
                "NossoNumero..............: "
                        + details.getNossoNumero()
        );

        System.out.println(
                "Amount...................: "
                        + details.getAmount()
        );

        System.out.println(
                "Initial status...........: "
                        + details.getStatus()
        );

        System.out.println(
                "DATABASE VALIDATION......: PASS"
        );

        System.out.println(
                "======================================================"
        );
    }
    private static PaymentScenarioExecution createExecution(
            AccountId accountId,
            String scenario
    ) {

        switch (scenario) {

            case "BELOW_MINIMUM":
                return FinancialScenarioRunner
                        .belowMinimumPayment(accountId);

            case "MINIMUM":
                return FinancialScenarioRunner
                        .minimumPayment(accountId);

            case "PARTIAL_20":
                return FinancialScenarioRunner
                        .partialPayment20(accountId);

            case "PARTIAL_60":
                return FinancialScenarioRunner
                        .partialPayment60(accountId);

            case "TOTAL":
                return FinancialScenarioRunner
                        .fullPayment(accountId);

            default:
                throw new IllegalArgumentException(
                        "Unsupported financial scenario: "
                                + scenario
                                + ". Use: BELOW_MINIMUM, MINIMUM, "
                                + "PARTIAL_20, PARTIAL_60 or TOTAL."
                );
        }
    }

    private static String requireProperty(
            String property
    ) {

        String value =
                System.getProperty(
                        property
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + property
                            + "=..."
            );
        }

        return value.trim();
    }
}