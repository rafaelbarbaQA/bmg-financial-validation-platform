package com.automation.infrastructure.purchase.execution;

import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.execution.PurchaseExecutor;
import com.automation.application.purchase.execution.PurchaseProcedureGateway;
import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.infrastructure.database.purchase.SqlServerPurchaseProcedureGateway;

import java.time.Clock;
import java.util.Objects;

public final class SqlServerPurchaseExecutor
        implements PurchaseExecutor {

    private final PurchaseProcedureGateway procedureGateway;
    private final Clock clock;

    public SqlServerPurchaseExecutor() {
        this(
                new SqlServerPurchaseProcedureGateway(),
                Clock.systemUTC()
        );
    }

    public SqlServerPurchaseExecutor(
            PurchaseProcedureGateway procedureGateway,
            Clock clock
    ) {
        this.procedureGateway =
                Objects.requireNonNull(
                        procedureGateway,
                        "Purchase procedure gateway cannot be null."
                );

        this.clock =
                Objects.requireNonNull(
                        clock,
                        "Clock cannot be null."
                );
    }

    @Override
    public PurchaseExecutionResult execute(
            PurchaseInstruction instruction
    ) {
        Objects.requireNonNull(
                instruction,
                "Purchase instruction cannot be null."
        );

        /*
         * Nenhuma chamada SQL pode ocorrer antes
         * da autorizacao explicita.
         */
        EffectfulExecutionGuard
                .exigirAutorizacao();

        procedureGateway.execute(
                instruction
        );

        return PurchaseExecutionResult.success(
                instruction,
                "Purchase executed through SPR_REALIZA_COMPRA.",
                clock.instant()
        );
    }
}