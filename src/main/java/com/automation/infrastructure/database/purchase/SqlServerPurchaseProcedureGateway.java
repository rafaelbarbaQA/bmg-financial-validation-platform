package com.automation.infrastructure.database.purchase;

import com.automation.application.purchase.execution.PurchaseProcedureGateway;
import com.automation.core.database.DatabaseAccessor;
import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.domain.purchase.PurchaseInstruction;

import java.sql.Date;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SqlServerPurchaseProcedureGateway
        implements PurchaseProcedureGateway {

    private static final String EXECUTE_PURCHASE =
            "SET NOCOUNT ON; "
                    + "DECLARE @BeforeId INT = "
                    + "ISNULL(CONVERT(INT, IDENT_CURRENT('dbo.EventosExternosComprasNaoProcessados')), 0); "

                    + "EXEC dbo.SPR_REALIZA_COMPRA "
                    + "?, ?, ?, ?, ?, ?, ?, ?; "

                    + "SELECT TOP 1 "
                    + "Id_EventoCompra AS ID_EVENTO_COMPRA "
                    + "FROM dbo.EventosExternosComprasNaoProcessados "
                    + "WHERE Id_EventoCompra > @BeforeId "
                    + "AND Id_Conta = ? "
                    + "AND Id_Operacao = ? "
                    + "AND ValorCompra = ? "
                    + "AND NumeroParcelas = ? "
                    + "AND ValorParcela = ? "
                    + "AND ValorContrato = ? "
                    + "AND CAST(DataCompra AS date) = ? "
                    + "AND CAST(DataEntradaCompra AS date) = ? "
                    + "AND Origem = 'TES' "
                    + "AND Status = 0 "
                    + "ORDER BY Id_EventoCompra DESC;";

    private final DatabaseAccessor databaseAccessor;

    public SqlServerPurchaseProcedureGateway() {
        this(
                new DatabaseAccessor()
        );
    }

    public SqlServerPurchaseProcedureGateway(
            DatabaseAccessor databaseAccessor
    ) {
        this.databaseAccessor =
                Objects.requireNonNull(
                        databaseAccessor,
                        "DatabaseAccessor cannot be null."
                );
    }

    @Override
    public void execute(
            PurchaseInstruction instruction
    ) {

        Objects.requireNonNull(
                instruction,
                "Purchase instruction cannot be null."
        );

        EffectfulExecutionGuard.exigirAutorizacao();

        long accountId =
                Long.parseLong(
                        instruction
                                .getAccountId()
                                .getValue()
                );

        int operationId =
                instruction
                        .getOperation()
                        .getOperationId();

        Date purchaseDate =
                Date.valueOf(
                        instruction.getPurchaseDate()
                );

        Date entryDate =
                Date.valueOf(
                        instruction.getEntryDate()
                );

        try {

            Optional<Map<String, Object>> result =
                    databaseAccessor.executeCommandReturningSingleRow(
                            EXECUTE_PURCHASE,

                            accountId,
                            instruction.getPurchaseAmount(),
                            instruction.getInstallmentCount(),
                            instruction.getInstallmentAmount(),
                            instruction.getContractAmount(),
                            operationId,
                            purchaseDate,
                            entryDate,

                            accountId,
                            operationId,
                            instruction.getPurchaseAmount(),
                            instruction.getInstallmentCount(),
                            instruction.getInstallmentAmount(),
                            instruction.getContractAmount(),
                            purchaseDate,
                            entryDate
                    );

            if (result.isEmpty()) {
                throw new IllegalStateException(
                        "SPR_REALIZA_COMPRA foi executada, "
                                + "mas nenhuma compra correspondente foi persistida "
                                + "para a conta "
                                + accountId
                                + "."
                );
            }

            Object purchaseEventId =
                    result.get().get(
                            "ID_EVENTO_COMPRA"
                    );

            if (!(purchaseEventId instanceof Number)) {
                throw new IllegalStateException(
                        "Id_EventoCompra nao foi retornado corretamente."
                );
            }

            long id =
                    ((Number) purchaseEventId)
                            .longValue();

            if (id <= 0) {
                throw new IllegalStateException(
                        "Id_EventoCompra invalido: "
                                + id
                );
            }

            System.out.println(
                    "SPR_REALIZA_COMPRA OK - IdEventoCompra: "
                            + id
            );

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not execute SPR_REALIZA_COMPRA "
                            + "for account "
                            + accountId
                            + ".",
                    exception
            );
        }
    }
}
