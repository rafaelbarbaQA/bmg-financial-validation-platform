package com.automation.integration.financial;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public final class FinancialSnapshotDatabaseIT {

    @Test
    public void shouldCaptureRealFinancialSnapshotFromDatabase() {

        String accountIdValue =
                requireProperty(
                        "snapshot.account.id"
                );

        AccountId accountId =
                new AccountId(
                        accountIdValue
                );

        FinancialSnapshotService service =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot snapshot =
                service.capture(
                        accountId
                );

        System.out.println();
        System.out.println(
                "=================================================="
        );

        System.out.println(
                " FINANCIAL SNAPSHOT - REAL DATABASE"
        );

        System.out.println(
                "=================================================="
        );

        System.out.println(
                "Conta.................: "
                        + snapshot.getAccountId().getValue()
        );

        System.out.println(
                "Status................: "
                        + snapshot.getAccountStatus()
        );

        System.out.println(
                "Historico.............: "
                        + snapshot.getHistoryId()
        );

        System.out.println(
                "Boleto................: "
                        + snapshot.getTicketId()
        );

        System.out.println(
                "NossoNumero...........: "
                        + snapshot.getNossoNumero()
        );

        System.out.println(
                "Saldo atual...........: "
                        + snapshot.getCurrentBalance()
        );

        System.out.println(
                "Valor minimo..........: "
                        + snapshot.getMinimumAmount()
        );

        System.out.println(
                "Valor fatura..........: "
                        + snapshot.getInvoiceAmount()
        );

        System.out.println(
                "Data previa...........: "
                        + snapshot.getPreviewDate()
        );

        System.out.println(
                "Corte esperado........: "
                        + snapshot.getExpectedCutoffDate()
        );

        System.out.println(
                "Vencimento............: "
                        + snapshot.getDueDate()
        );

        System.out.println(
                "Capturado em..........: "
                        + snapshot.getCapturedAt()
        );

        System.out.println(
                "=================================================="
        );

        assertNotNull(
                snapshot
        );

        assertEquals(
                snapshot.getAccountId(),
                accountId
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