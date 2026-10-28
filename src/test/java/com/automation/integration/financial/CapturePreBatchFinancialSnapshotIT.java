package com.automation.integration.financial;

import io.qameta.allure.Story;
import io.qameta.allure.Feature;
import io.qameta.allure.Epic;
import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.model.PaymentEventDetails;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.database.repository.SqlServerPaymentEventDetailsRepository;
import com.automation.infrastructure.financial.snapshot.FinancialSnapshotFileStore;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
@Epic("Financial Validation Platform")
@Feature("Ciclo financeiro")

public final class CapturePreBatchFinancialSnapshotIT {

    @Test
@Story("Snapshot pre-batch")
    public void shouldCapturePreBatchFinancialBaseline() {

        AccountId accountId =
                new AccountId(
                        requireProperty(
                                "validation.account.id"
                        )
                );

        long paymentEventId =
                Long.parseLong(
                        requireProperty(
                                "payment.event.id"
                        )
                );

        int expectedPaymentStatus =
                Integer.parseInt(
                        System.getProperty(
                                "expected.payment.status",
                                "0"
                        )
                );

        PaymentEventDetails payment =
                new SqlServerPaymentEventDetailsRepository()
                        .findByPaymentEventId(
                                paymentEventId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment event not found: "
                                                + paymentEventId
                                )
                        );

        assertEquals(
                payment.getAccountId().getValue(),
                accountId.getValue(),
                "Payment event belongs to another account."
        );

        assertEquals(
                payment.getStatus(),
                expectedPaymentStatus,
                "Payment is not in the expected pre-batch status."
        );

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot snapshot =
                snapshotService.capture(
                        accountId
                );

        FinancialSnapshotFileStore store =
                new FinancialSnapshotFileStore();

        Path path =
                store.saveBefore(
                        snapshot
                );

        assertTrue(
                Files.exists(path),
                "Pre-batch financial snapshot file was not created."
        );

        System.out.println();
        System.out.println(
                "================ PRE-BATCH FINANCIAL BASELINE ================"
        );

        System.out.println(
                "Conta.....................: "
                        + snapshot.getAccountId().getValue()
        );

        System.out.println(
                "Evento pagamento..........: "
                        + payment.getPaymentEventId()
        );

        System.out.println(
                "Status pagamento..........: "
                        + payment.getStatus()
        );

        System.out.println(
                "Saldo atual...............: "
                        + snapshot.getCurrentBalance()
        );

        System.out.println(
                "Saldo fatura..............: "
                        + snapshot.getInvoiceAmount()
        );

        System.out.println(
                "Valor minimo..............: "
                        + snapshot.getMinimumAmount()
        );

        System.out.println(
                "NossoNumero...............: "
                        + snapshot.getNossoNumero()
        );

        System.out.println(
                "Arquivo BEFORE............: "
                        + path.toAbsolutePath()
        );

        System.out.println(
                "PRE-BATCH BASELINE........: CAPTURED"
        );

        System.out.println(
                "=============================================================="
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