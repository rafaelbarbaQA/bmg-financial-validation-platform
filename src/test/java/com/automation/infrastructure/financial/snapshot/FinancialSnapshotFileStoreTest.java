package com.automation.infrastructure.financial.snapshot;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class FinancialSnapshotFileStoreTest {

    @Test
    public void shouldPersistAndLoadBeforeSnapshot() throws Exception {

        Path directory =
                Files.createTempDirectory(
                        "financial-snapshot-test"
                );

        AccountId accountId =
                new AccountId(
                        "13714323"
                );

        FinancialSnapshot original =
                new FinancialSnapshot(
                        accountId,
                        0,
                        "877716729",
                        "457854667",
                        "NOSSO-NUMERO-001",
                        new BigDecimal("1000.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 7, 10),
                        LocalDate.of(2026, 7, 10),
                        Instant.parse(
                                "2026-08-16T12:00:00Z"
                        )
                );

        FinancialSnapshotFileStore store =
                new FinancialSnapshotFileStore(
                        directory
                );

        try {

            Path savedPath =
                    store.saveBefore(
                            original
                    );

            assertTrue(
                    Files.exists(savedPath),
                    "BEFORE snapshot file should exist."
            );

            FinancialSnapshot loaded =
                    store.loadBefore(
                            accountId
                    );

            assertEquals(
                    loaded.getAccountId(),
                    original.getAccountId()
            );

            assertEquals(
                    loaded.getAccountStatus(),
                    original.getAccountStatus()
            );

            assertEquals(
                    loaded.getHistoryId(),
                    original.getHistoryId()
            );

            assertEquals(
                    loaded.getTicketId(),
                    original.getTicketId()
            );

            assertEquals(
                    loaded.getNossoNumero(),
                    original.getNossoNumero()
            );

            assertEquals(
                    loaded.getCurrentBalance(),
                    original.getCurrentBalance()
            );

            assertEquals(
                    loaded.getMinimumAmount(),
                    original.getMinimumAmount()
            );

            assertEquals(
                    loaded.getInvoiceAmount(),
                    original.getInvoiceAmount()
            );

            assertEquals(
                    loaded.getPreviewDate(),
                    original.getPreviewDate()
            );

            assertEquals(
                    loaded.getExpectedCutoffDate(),
                    original.getExpectedCutoffDate()
            );

            assertEquals(
                    loaded.getDueDate(),
                    original.getDueDate()
            );

            assertEquals(
                    loaded.getCapturedAt(),
                    original.getCapturedAt()
            );

        } finally {

            Files.deleteIfExists(
                    store.beforePath(accountId)
            );

            Files.deleteIfExists(
                    directory
            );
        }
    }
}