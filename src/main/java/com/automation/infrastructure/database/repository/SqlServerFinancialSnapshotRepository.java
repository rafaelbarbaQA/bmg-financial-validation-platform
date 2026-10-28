package com.automation.infrastructure.database.repository;

import com.automation.application.financial.snapshot.FinancialSnapshotRepository;
import com.automation.core.database.DatabaseAccessor;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SqlServerFinancialSnapshotRepository
        implements FinancialSnapshotRepository {

    private static final int MONTHLY_INVOICE_TYPE = 7;

    private static final String FIND_SNAPSHOT =
            "SELECT TOP (1) "
                    + "c.Id_Conta AS ACCOUNT_ID, "
                    + "c.Status AS ACCOUNT_STATUS, "
                    + "hc.Id_Historico AS HISTORY_ID, "
                    + "hc.saldoAtualFinal AS CURRENT_BALANCE, "
                    + "hc.valorMinimoExtrato AS MINIMUM_AMOUNT, "
                    + "be.Id_Boleto AS TICKET_ID, "
                    + "be.NossoNumero AS NOSSO_NUMERO, "
                    + "be.valorBoleto AS INVOICE_AMOUNT, "
                    + "cvp.DataPrevia AS PREVIEW_DATE, "
                    + "cv.DataPrevistaCorte AS EXPECTED_CUTOFF_DATE, "
                    + "ec.ProximoVencimentoPadrao AS DUE_DATE "
                    + "FROM Contas c "
                    + "INNER JOIN EstadosContas ec "
                    + "        ON ec.Id_Conta = c.Id_Conta "
                    + "INNER JOIN HistoricosCorrentes hc "
                    + "        ON hc.Id_Conta = c.Id_Conta "
                    + "INNER JOIN BoletosEmitidos be "
                    + "        ON be.Id_Conta = hc.Id_Conta "
                    + "       AND be.Id_Historico = hc.Id_Historico "
                    + "LEFT JOIN ControleVencimentos cv "
                    + "       ON cv.VencimentoPadraoFaturar = ec.ProximoVencimentoPadrao "
                    + "LEFT JOIN ControleVencimentosProdutos cvp "
                    + "       ON cvp.Id_Produto = ec.Id_Produto "
                    + "      AND cvp.DataVencimento = CONVERT(smalldatetime, ec.ProximoVencimentoPadrao, 103) "
                    + "WHERE c.Id_Conta = ? "
                    + "  AND be.Id_TipoBoleto = ? "
                    + "ORDER BY hc.Id_Historico DESC, "
                    + "         be.Id_Boleto DESC";

    private final DatabaseAccessor databaseAccessor;

    public SqlServerFinancialSnapshotRepository() {
        this(
                new DatabaseAccessor()
        );
    }

    public SqlServerFinancialSnapshotRepository(
            DatabaseAccessor databaseAccessor
    ) {

        this.databaseAccessor =
                Objects.requireNonNull(
                        databaseAccessor,
                        "DatabaseAccessor cannot be null."
                );
    }

    @Override
    public FinancialSnapshot capture(
            AccountId accountId
    ) {

        Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        long numericAccountId =
                parseAccountId(
                        accountId
                );

        try {

            Optional<Map<String, Object>> result =
                    databaseAccessor.executeQuery(
                            FIND_SNAPSHOT,
                            numericAccountId,
                            MONTHLY_INVOICE_TYPE
                    );

            if (result.isEmpty()) {

                throw new IllegalStateException(
                        "Financial snapshot data not found for account "
                                + accountId.getValue()
                                + "."
                );
            }

            Map<String, Object> row =
                    result.get();

            return new FinancialSnapshot(
                    accountId,
                    requireNumber(
                            row,
                            "ACCOUNT_STATUS"
                    ).intValue(),
                    requireText(
                            row,
                            "HISTORY_ID"
                    ),
                    requireText(
                            row,
                            "TICKET_ID"
                    ),
                    requireText(
                            row,
                            "NOSSO_NUMERO"
                    ),
                    requireBigDecimal(
                            row,
                            "CURRENT_BALANCE"
                    ),
                    requireBigDecimal(
                            row,
                            "MINIMUM_AMOUNT"
                    ),
                    requireBigDecimal(
                            row,
                            "INVOICE_AMOUNT"
                    ),
                    optionalLocalDate(
                            row,
                            "PREVIEW_DATE"
                    ),
                    requireLocalDate(
                            row,
                            "EXPECTED_CUTOFF_DATE"
                    ),
                    requireLocalDate(
                            row,
                            "DUE_DATE"
                    ),
                    Instant.now()
            );

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not capture financial snapshot for account "
                            + accountId.getValue()
                            + ".",
                    exception
            );
        }
    }

    private static long parseAccountId(
            AccountId accountId
    ) {

        try {

            long value =
                    Long.parseLong(
                            accountId.getValue()
                    );

            if (value <= 0) {

                throw new IllegalArgumentException(
                        "Account ID must be greater than zero."
                );
            }

            return value;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Account ID must be numeric: "
                            + accountId.getValue(),
                    exception
            );
        }
    }

    private static Number requireNumber(
            Map<String, Object> row,
            String column
    ) {

        Object value =
                row.get(column);

        if (!(value instanceof Number)) {

            throw new IllegalStateException(
                    column
                            + " was not returned as a numeric value. Actual: "
                            + value
            );
        }

        return (Number) value;
    }

    private static BigDecimal requireBigDecimal(
            Map<String, Object> row,
            String column
    ) {

        Object value =
                row.get(column);

        if (value == null) {

            throw new IllegalStateException(
                    column
                            + " was returned as null."
            );
        }

        if (value instanceof BigDecimal) {

            return (BigDecimal) value;
        }

        if (value instanceof Number) {

            return new BigDecimal(
                    value.toString()
            );
        }

        throw new IllegalStateException(
                column
                        + " was not returned as a numeric value. Actual: "
                        + value
        );
    }

    private static String requireText(
            Map<String, Object> row,
            String column
    ) {

        Object value =
                row.get(column);

        if (value == null) {

            throw new IllegalStateException(
                    column
                            + " was returned as null."
            );
        }

        String text =
                value.toString()
                        .trim();

        if (text.isEmpty()) {

            throw new IllegalStateException(
                    column
                            + " was returned as blank."
            );
        }

        return text;
    }

    private static LocalDate optionalLocalDate(
            Map<String, Object> row,
            String column
    ) {

        Object value =
                row.get(column);

        if (value == null) {
            return null;
        }

        if (value instanceof String
                && ((String) value).trim().isEmpty()) {

            return null;
        }

        return requireLocalDate(
                row,
                column
        );
    }
    private static LocalDate requireLocalDate(
            Map<String, Object> row,
            String column
    ) {

        Object value =
                row.get(column);

        if (value == null) {

            throw new IllegalStateException(
                    column
                            + " was returned as null."
            );
        }

        if (value instanceof Date) {

            return ((Date) value)
                    .toLocalDate();
        }

        if (value instanceof Timestamp) {

            return ((Timestamp) value)
                    .toLocalDateTime()
                    .toLocalDate();
        }

        if (value instanceof LocalDate) {

            return (LocalDate) value;
        }

        if (value instanceof LocalDateTime) {

            return ((LocalDateTime) value)
                    .toLocalDate();
        }

        if (value instanceof String) {

            String text =
                    ((String) value)
                            .trim();

            if (text.isEmpty()) {

                throw new IllegalStateException(
                        column
                                + " was returned as blank."
                );
            }

            DateTimeFormatter[] supportedFormats = {
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ISO_LOCAL_DATE
            };

            for (DateTimeFormatter formatter : supportedFormats) {

                try {

                    return LocalDate.parse(
                            text,
                            formatter
                    );

                } catch (DateTimeParseException ignored) {
                    // Try next supported format.
                }
            }

            throw new IllegalStateException(
                    column
                            + " could not be parsed as LocalDate. Value: "
                            + text
            );
        }

        throw new IllegalStateException(
                column
                        + " could not be converted to LocalDate. Type: "
                        + value.getClass().getName()
                        + " | Value: "
                        + value
        );
    }
}