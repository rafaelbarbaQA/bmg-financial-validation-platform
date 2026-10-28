package com.automation.infrastructure.financial.snapshot;

import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;

public final class FinancialSnapshotFileStore {

    private static final String DEFAULT_DIRECTORY =
            ".financial-snapshots";

    private final Path directory;

    public FinancialSnapshotFileStore() {
        this(
                Paths.get(
                        System.getProperty(
                                "snapshot.dir",
                                DEFAULT_DIRECTORY
                        )
                )
        );
    }

    public FinancialSnapshotFileStore(
            Path directory
    ) {
        this.directory =
                Objects.requireNonNull(
                        directory,
                        "Snapshot directory cannot be null."
                );
    }

    public Path saveBefore(
            FinancialSnapshot snapshot
    ) {

        Objects.requireNonNull(
                snapshot,
                "Financial snapshot cannot be null."
        );

        try {

            Files.createDirectories(
                    directory
            );

            Path path =
                    beforePath(
                            snapshot.getAccountId()
                    );

            Properties properties =
                    toProperties(
                            snapshot
                    );

            try (OutputStream output =
                         Files.newOutputStream(path)) {

                properties.store(
                        output,
                        "Financial snapshot BEFORE"
                );
            }

            return path;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not persist BEFORE financial snapshot for account "
                            + snapshot.getAccountId().getValue()
                            + ".",
                    exception
            );
        }
    }

    public FinancialSnapshot loadBefore(
            AccountId accountId
    ) {

        Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        Path path =
                beforePath(
                        accountId
                );

        if (!Files.exists(path)) {

            throw new IllegalStateException(
                    "BEFORE snapshot does not exist for account "
                            + accountId.getValue()
                            + ". Expected file: "
                            + path.toAbsolutePath()
            );
        }

        Properties properties =
                new Properties();

        try (InputStream input =
                     Files.newInputStream(path)) {

            properties.load(
                    input
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Could not load BEFORE financial snapshot for account "
                            + accountId.getValue()
                            + ".",
                    exception
            );
        }

        String storedAccountId =
                requireProperty(
                        properties,
                        "accountId"
                );

        if (!accountId.getValue()
                .equals(storedAccountId)) {

            throw new IllegalStateException(
                    "Stored snapshot belongs to account "
                            + storedAccountId
                            + ", but "
                            + accountId.getValue()
                            + " was requested."
            );
        }

        return new FinancialSnapshot(
                accountId,
                Integer.parseInt(
                        requireProperty(
                                properties,
                                "accountStatus"
                        )
                ),
                requireProperty(
                        properties,
                        "historyId"
                ),
                requireProperty(
                        properties,
                        "ticketId"
                ),
                requireProperty(
                        properties,
                        "nossoNumero"
                ),
                new BigDecimal(
                        requireProperty(
                                properties,
                                "currentBalance"
                        )
                ),
                new BigDecimal(
                        requireProperty(
                                properties,
                                "minimumAmount"
                        )
                ),
                new BigDecimal(
                        requireProperty(
                                properties,
                                "invoiceAmount"
                        )
                ),
                optionalLocalDate(
                        properties,
                        "previewDate"
                ),
                LocalDate.parse(
                        requireProperty(
                                properties,
                                "expectedCutoffDate"
                        )
                ),
                LocalDate.parse(
                        requireProperty(
                                properties,
                                "dueDate"
                        )
                ),
                Instant.parse(
                        requireProperty(
                                properties,
                                "capturedAt"
                        )
                )
        );
    }

    public Path beforePath(
            AccountId accountId
    ) {

        Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        return directory.resolve(
                accountId.getValue()
                        + "-before.properties"
        );
    }

    private static Properties toProperties(
            FinancialSnapshot snapshot
    ) {

        Properties properties =
                new Properties();

        properties.setProperty(
                "accountId",
                snapshot.getAccountId().getValue()
        );

        properties.setProperty(
                "accountStatus",
                String.valueOf(
                        snapshot.getAccountStatus()
                )
        );

        properties.setProperty(
                "historyId",
                snapshot.getHistoryId()
        );

        properties.setProperty(
                "ticketId",
                snapshot.getTicketId()
        );

        properties.setProperty(
                "nossoNumero",
                snapshot.getNossoNumero()
        );

        properties.setProperty(
                "currentBalance",
                snapshot.getCurrentBalance().toPlainString()
        );

        properties.setProperty(
                "minimumAmount",
                snapshot.getMinimumAmount().toPlainString()
        );

        properties.setProperty(
                "invoiceAmount",
                snapshot.getInvoiceAmount().toPlainString()
        );

        if (snapshot.getPreviewDate() != null) {

            properties.setProperty(
                    "previewDate",
                    snapshot.getPreviewDate().toString()
            );
        }

        properties.setProperty(
                "expectedCutoffDate",
                snapshot.getExpectedCutoffDate().toString()
        );

        properties.setProperty(
                "dueDate",
                snapshot.getDueDate().toString()
        );

        properties.setProperty(
                "capturedAt",
                snapshot.getCapturedAt().toString()
        );

        return properties;
    }

    private static LocalDate optionalLocalDate(
            Properties properties,
            String name
    ) {

        String value =
                properties.getProperty(
                        name
                );

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        return LocalDate.parse(
                value.trim()
        );
    }
    private static String requireProperty(
            Properties properties,
            String name
    ) {

        String value =
                properties.getProperty(
                        name
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalStateException(
                    "Snapshot property "
                            + name
                            + " is missing."
            );
        }

        return value.trim();
    }
}