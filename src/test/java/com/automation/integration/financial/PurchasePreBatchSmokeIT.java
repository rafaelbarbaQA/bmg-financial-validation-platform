package com.automation.integration.financial;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import com.automation.infrastructure.database.purchase.SqlServerPurchasePersistenceRepository;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.financial.snapshot.FinancialSnapshotFileStore;
import com.automation.infrastructure.purchase.execution.SqlServerPurchaseExecutor;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Epic("Financial Validation Platform")
@Feature("PRE-BATCH")
@Story("Compra real com validacao de persistencia")
@Severity(SeverityLevel.CRITICAL)
public final class PurchasePreBatchSmokeIT {

    @Test(groups = { "effectful", "integration", "database" })
    @Description(
            "Captura o snapshot BEFORE, gera a instrucao de compra, "
                    + "executa SPR_REALIZA_COMPRA e valida o Id_EventoCompra persistido."
    )
    public void shouldExecuteRealPurchaseAndValidatePersistence() {

        String environment =
                requireProperty("env");

        String expectedEnvironment =
                System.getProperty(
                        "purchase.smoke.expected.env",
                        "102"
                ).trim();

        if (!expectedEnvironment.equals(environment)) {
            throw new IllegalStateException(
                    "Ambiente bloqueado para compra real. Esperado env="
                            + expectedEnvironment
                            + ", recebido env="
                            + environment
                            + "."
            );
        }

        String accountValue =
                requireProperty(
                        "purchase.smoke.account"
                );

        int operationId =
                Integer.parseInt(
                        requireProperty(
                                "purchase.smoke.operation"
                        )
                );

        LocalDate movementDate =
                LocalDate.parse(
                        requireProperty(
                                "purchase.smoke.date"
                        )
                );

        long seed =
                Long.parseLong(
                        System.getProperty(
                                "purchase.smoke.seed",
                                "20260723"
                        )
                );

        AccountId accountId =
                new AccountId(
                        accountValue
                );

        PurchaseOperation operation =
                PurchaseOperation.fromOperationId(
                        operationId
                );

        Allure.parameter("Fase", "PRE-BATCH");
        Allure.parameter("Fluxo", "COMPRA");
        Allure.parameter("Ambiente", environment);
        Allure.parameter("Conta", accountValue);
        Allure.parameter("IdOperacao", operationId);
        Allure.parameter("Data movimento", movementDate);

        Allure.step(
                "Capturar snapshot financeiro BEFORE"
        );

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot before =
                snapshotService.capture(
                        accountId
                );

        assertNotNull(
                before,
                "Snapshot BEFORE nao pode ser nulo."
        );

        FinancialSnapshotFileStore snapshotStore =
                new FinancialSnapshotFileStore();

        Path beforePath =
                snapshotStore.saveBefore(
                        before
                );

        assertTrue(
                beforePath.toFile().exists(),
                "Arquivo de snapshot BEFORE nao foi criado."
        );

        Allure.parameter(
                "Snapshot BEFORE",
                beforePath.toAbsolutePath().toString()
        );

        Allure.step(
                "Gerar instrucao de compra PRE-BATCH"
        );

        PurchaseInstructionGenerator generator =
                new PurchaseInstructionGenerator(
                        seed,
                        movementDate
                );

        PurchaseInstruction instruction =
                generator.generate(
                        accountId,
                        operation
                );

        assertNotNull(
                instruction,
                "Instrucao de compra nao pode ser nula."
        );

        Allure.parameter(
                "Valor compra",
                instruction.getPurchaseAmount()
        );

        Allure.parameter(
                "Numero parcelas",
                instruction.getInstallmentCount()
        );

        Allure.parameter(
                "Valor parcela",
                instruction.getInstallmentAmount()
        );

        Allure.parameter(
                "Valor contrato",
                instruction.getContractAmount()
        );

        SqlServerPurchasePersistenceRepository persistenceRepository =
                new SqlServerPurchasePersistenceRepository();

        Allure.step(
                "Capturar baseline de Id_EventoCompra"
        );

        long baselineId =
                persistenceRepository.findCurrentMaxEventId();

        Allure.parameter(
                "Baseline IdEventoCompra",
                baselineId
        );

        /*
         * Segunda barreira antes da operacao real.
         * O SqlServerPurchaseExecutor tambem possui o guard.
         */
        EffectfulExecutionGuard.exigirAutorizacao();

        Allure.step(
                "Executar SPR_REALIZA_COMPRA"
        );

        PurchaseExecutionResult executionResult =
                new SqlServerPurchaseExecutor()
                        .execute(
                                instruction
                        );

        assertNotNull(
                executionResult,
                "Resultado da execucao da compra nao pode ser nulo."
        );

        Allure.step(
                "Localizar compra persistida em EventosExternosComprasNaoProcessados"
        );

        long purchaseEventId =
                waitForPurchaseEventId(
                        persistenceRepository,
                        instruction,
                        baselineId
                );

        assertTrue(
                purchaseEventId > baselineId,
                "IdEventoCompra deve ser maior que o baseline."
        );

        Allure.parameter(
                "IdEventoCompra",
                purchaseEventId
        );

        Allure.step(
                "Compra persistida com IdEventoCompra: "
                        + purchaseEventId
        );

        attachEvidence(
                environment,
                before,
                beforePath,
                instruction,
                baselineId,
                purchaseEventId
        );

        System.out.println();
        System.out.println("============================================================");
        System.out.println(" FINANCIAL PLATFORM - PRE-BATCH PURCHASE SMOKE");
        System.out.println("============================================================");
        System.out.println("Ambiente.................: " + environment);
        System.out.println("Conta....................: " + accountValue);
        System.out.println("IdOperacao...............: " + operationId);
        System.out.println("Data movimento...........: " + movementDate);
        System.out.println("Saldo BEFORE.............: " + before.getCurrentBalance());
        System.out.println("Fatura BEFORE............: " + before.getInvoiceAmount());
        System.out.println("Minimo BEFORE............: " + before.getMinimumAmount());
        System.out.println("Valor compra.............: " + instruction.getPurchaseAmount());
        System.out.println("Parcelas.................: " + instruction.getInstallmentCount());
        System.out.println("Valor parcela............: " + instruction.getInstallmentAmount());
        System.out.println("Valor contrato...........: " + instruction.getContractAmount());
        System.out.println("Baseline IdCompra........: " + baselineId);
        System.out.println("IdEventoCompra...........: " + purchaseEventId);
        System.out.println("Resultado................: PASS");
        System.out.println("============================================================");
    }

    private static long waitForPurchaseEventId(
            SqlServerPurchasePersistenceRepository repository,
            PurchaseInstruction instruction,
            long baselineId
    ) {

        for (int attempt = 1; attempt <= 5; attempt++) {

            Optional<Long> result =
                    repository.findLatestMatchingEventIdAfter(
                            instruction,
                            baselineId
                    );

            if (result.isPresent()) {
                return result.get();
            }

            try {
                Thread.sleep(500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Execucao interrompida aguardando persistencia da compra.",
                        exception
                );
            }
        }

        throw new IllegalStateException(
                "Compra executada, mas nenhum novo registro correspondente "
                        + "foi encontrado em EventosExternosComprasNaoProcessados."
        );
    }

    private static void attachEvidence(
            String environment,
            FinancialSnapshot before,
            Path beforePath,
            PurchaseInstruction instruction,
            long baselineId,
            long purchaseEventId
    ) {

        String html =
                "<html><body>"
                        + "<h2>PRE-BATCH - Compra Real</h2>"
                        + "<h3>Snapshot BEFORE</h3>"
                        + "<table border=\"1\" cellpadding=\"7\" cellspacing=\"0\">"
                        + row("Ambiente", environment)
                        + row("Conta", before.getAccountId().getValue())
                        + row("Status conta", before.getAccountStatus())
                        + row("Historico", before.getHistoryId())
                        + row("IdBoleto BEFORE", before.getTicketId())
                        + row("NossoNumero", before.getNossoNumero())
                        + row("Saldo atual", before.getCurrentBalance())
                        + row("Valor minimo", before.getMinimumAmount())
                        + row("Valor fatura", before.getInvoiceAmount())
                        + row("Arquivo snapshot", beforePath.toAbsolutePath())
                        + "</table>"
                        + "<h3>Instrucao de Compra</h3>"
                        + "<table border=\"1\" cellpadding=\"7\" cellspacing=\"0\">"
                        + row("IdOperacao", instruction.getOperation().getOperationId())
                        + row("Valor compra", instruction.getPurchaseAmount())
                        + row("Numero parcelas", instruction.getInstallmentCount())
                        + row("Valor parcela", instruction.getInstallmentAmount())
                        + row("Valor contrato", instruction.getContractAmount())
                        + row("Data compra", instruction.getPurchaseDate())
                        + row("Data entrada", instruction.getEntryDate())
                        + "</table>"
                        + "<h3>Persistencia</h3>"
                        + "<table border=\"1\" cellpadding=\"7\" cellspacing=\"0\">"
                        + row("Tabela", "EventosExternosComprasNaoProcessados")
                        + row("Origem esperada", "TES")
                        + row("Status esperado", "0")
                        + row("Baseline IdEventoCompra", baselineId)
                        + row("IdEventoCompra gerado", purchaseEventId)
                        + row("Resultado", "PASS")
                        + "</table>"
                        + "</body></html>";

        Allure.addAttachment(
                "PRE-BATCH - Compra - Evidencia",
                "text/html",
                html
        );
    }

    private static String row(
            String field,
            Object value
    ) {
        return "<tr><td><strong>"
                + escape(field)
                + "</strong></td><td>"
                + escape(value)
                + "</td></tr>";
    }

    private static String escape(
            Object value
    ) {
        if (value == null) {
            return "";
        }

        return String.valueOf(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String requireProperty(
            String name
    ) {
        String value =
                System.getProperty(name);

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
