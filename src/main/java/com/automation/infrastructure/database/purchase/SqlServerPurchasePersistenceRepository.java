package com.automation.infrastructure.database.purchase;

import com.automation.core.database.DatabaseAccessor;
import com.automation.domain.purchase.PurchaseInstruction;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SqlServerPurchasePersistenceRepository {

    private static final String FIND_PURCHASE =
            "SELECT TOP 1 "
                    + "Id_EventoCompra AS ID_EVENTO_COMPRA "
                    + "FROM EventosExternosComprasNaoProcessados "
                    + "WHERE Id_Conta = ? "
                    + "AND Id_Operacao = ? "
                    + "AND ValorCompra = ? "
                    + "AND NumeroParcelas = ? "
                    + "AND ValorParcela = ? "
                    + "AND ValorContrato = ? "
                    + "AND CAST(DataCompra AS date) = ? "
                    + "AND CAST(DataEntradaCompra AS date) = ? "
                    + "AND Origem = 'TES' "
                    + "AND Status = 0 "
                    + "AND Id_EventoCompra > ? "
                    + "ORDER BY Id_EventoCompra DESC";

    private final DatabaseAccessor databaseAccessor;

    public SqlServerPurchasePersistenceRepository() {
        this(
                new DatabaseAccessor()
        );
    }

    public SqlServerPurchasePersistenceRepository(
            DatabaseAccessor databaseAccessor
    ) {
        this.databaseAccessor =
                Objects.requireNonNull(
                        databaseAccessor,
                        "DatabaseAccessor cannot be null."
                );
    }

    public long findCurrentMaxEventId() {

        try {
            Optional<Map<String, Object>> row =
                    databaseAccessor.executeQuery(
                            "SELECT ISNULL(MAX(Id_EventoCompra), 0) AS ID_EVENTO_COMPRA "
                                    + "FROM EventosExternosComprasNaoProcessados"
                    );

            if (row.isEmpty()) {
                return 0L;
            }

            Object value =
                    row.get().get(
                            "ID_EVENTO_COMPRA"
                    );

            if (value == null) {
                return 0L;
            }

            return value instanceof Number
                    ? ((Number) value).longValue()
                    : Long.parseLong(value.toString());

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not read current maximum purchase event ID.",
                    exception
            );
        }
    }

    public Optional<Long> findLatestMatchingEventIdAfter(
            PurchaseInstruction instruction,
            long minimumExclusiveId
    ) {

        Objects.requireNonNull(
                instruction,
                "Purchase instruction cannot be null."
        );

        if (minimumExclusiveId < 0) {
            throw new IllegalArgumentException(
                    "Minimum purchase event ID cannot be negative."
            );
        }

        long accountId =
                Long.parseLong(
                        instruction
                                .getAccountId()
                                .getValue()
                );

        try {

            Optional<Map<String, Object>> row =
                    databaseAccessor.executeQuery(
                            FIND_PURCHASE,
                            accountId,
                            instruction.getOperation().getOperationId(),
                            instruction.getPurchaseAmount(),
                            instruction.getInstallmentCount(),
                            instruction.getInstallmentAmount(),
                            instruction.getContractAmount(),
                            Date.valueOf(instruction.getPurchaseDate()),
                            Date.valueOf(instruction.getEntryDate()),
                            minimumExclusiveId
                    );

            if (row.isEmpty()) {
                return Optional.empty();
            }

            Object value =
                    row.get().get(
                            "ID_EVENTO_COMPRA"
                    );

            if (value == null) {
                return Optional.empty();
            }

            if (value instanceof Number) {
                return Optional.of(
                        ((Number) value).longValue()
                );
            }

            return Optional.of(
                    Long.parseLong(
                            value.toString()
                    )
            );

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not locate persisted purchase for account "
                            + accountId
                            + ".",
                    exception
            );
        }
    }
}
