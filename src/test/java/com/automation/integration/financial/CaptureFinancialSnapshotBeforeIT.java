package com.automation.integration.financial;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.financial.snapshot.FinancialSnapshotFileStore;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Epic("Financial Validation Platform")
@Feature("PRE-BATCH")
@Story("Captura do estado financeiro antes da operação")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public final class CaptureFinancialSnapshotBeforeIT {

    @Test(groups = { "integration", "database" })
    @Description(
            "Captura o estado financeiro real da conta antes da operação, "
                    + "persiste o snapshot BEFORE e registra as evidências no Allure."
    )
    public void shouldCaptureAndPersistBeforeSnapshot() {

        String accountValue =
                requireProperty(
                        "snapshot.account.id"
                );

        String environment =
                System.getProperty(
                        "env",
                        "NAO_INFORMADO"
                );

        Allure.parameter(
                "Fase",
                "PRE-BATCH"
        );

        Allure.parameter(
                "Ambiente",
                environment
        );

        Allure.parameter(
                "Conta",
                accountValue
        );

        AccountId accountId =
                new AccountId(
                        accountValue
                );

        Allure.step(
                "Consultar estado financeiro atual da conta"
        );

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot snapshot =
                snapshotService.capture(
                        accountId
                );

        assertNotNull(
                snapshot,
                "O snapshot PRE-BATCH nao pode ser nulo."
        );

        Allure.step(
                "Snapshot PRE-BATCH capturado com sucesso"
        );

        FinancialSnapshotFileStore store =
                new FinancialSnapshotFileStore();

        Path path =
                store.saveBefore(
                        snapshot
                );

        assertTrue(
                path.toFile().exists(),
                "BEFORE snapshot file was not created."
        );

        Allure.parameter(
                "Snapshot",
                "CAPTURADO E PERSISTIDO"
        );

        Allure.parameter(
                "Arquivo BEFORE",
                path.toAbsolutePath().toString()
        );

        anexarSnapshot(
                environment,
                snapshot,
                path
        );

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
                "Ambiente..............: " + environment
        );
        System.out.println(
                "Conta.................: " + snapshot.getAccountId().getValue()
        );
        System.out.println(
                "Status................: " + snapshot.getAccountStatus()
        );
        System.out.println(
                "Historico.............: " + snapshot.getHistoryId()
        );
        System.out.println(
                "Boleto................: " + snapshot.getTicketId()
        );
        System.out.println(
                "NossoNumero...........: " + snapshot.getNossoNumero()
        );
        System.out.println(
                "Saldo.................: " + snapshot.getCurrentBalance()
        );
        System.out.println(
                "Minimo................: " + snapshot.getMinimumAmount()
        );
        System.out.println(
                "Fatura................: " + snapshot.getInvoiceAmount()
        );
        System.out.println(
                "Arquivo BEFORE........: " + path.toAbsolutePath()
        );
        System.out.println(
                "Resultado.............: PRE-BATCH OK"
        );
        System.out.println(
                "============================================================"
        );
    }

    private static void anexarSnapshot(
            String environment,
            FinancialSnapshot snapshot,
            Path path
    ) {

        String html =
                "<html><body>"
                        + "<h2>Financial Snapshot - PRE-BATCH</h2>"
                        + "<table border='1' cellpadding='8' cellspacing='0'>"
                        + "<tr><th align='left'>Campo</th><th align='left'>Valor</th></tr>"
                        + row("Ambiente", environment)
                        + row("Fase", "PRE-BATCH")
                        + row("Conta", snapshot.getAccountId().getValue())
                        + row("Status da conta", snapshot.getAccountStatus())
                        + row("Historico", snapshot.getHistoryId())
                        + row("IdBoleto", snapshot.getTicketId())
                        + row("NossoNumero", snapshot.getNossoNumero())
                        + row("Saldo atual", snapshot.getCurrentBalance())
                        + row("Valor minimo", snapshot.getMinimumAmount())
                        + row("Valor da fatura", snapshot.getInvoiceAmount())
                        + row("Arquivo BEFORE", path.toAbsolutePath())
                        + "</table>"
                        + "<p><strong>Resultado:</strong> snapshot PRE-BATCH capturado e persistido com sucesso.</p>"
                        + "</body></html>";

        Allure.addAttachment(
                "Financial Snapshot - PRE-BATCH",
                "text/html",
                html,
                ".html"
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
