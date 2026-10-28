package com.automation.infrastructure.purchase.execution;

import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.execution.PurchaseProcedureGateway;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public final class SqlServerPurchaseExecutorTest {

    @AfterMethod
    public void clearEffectfulAuthorization() {
        System.clearProperty(
                "permitirOperacoesComEfeito"
        );
    }

    @Test
    public void shouldBlockExecutionWithoutExplicitAuthorization() {

        AtomicBoolean procedureCalled =
                new AtomicBoolean(false);

        PurchaseProcedureGateway gateway =
                instruction ->
                        procedureCalled.set(true);

        SqlServerPurchaseExecutor executor =
                new SqlServerPurchaseExecutor(
                        gateway,
                        fixedClock()
                );

        expectThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        instruction()
                )
        );

        assertFalse(
                procedureCalled.get()
        );
    }

    @Test
    public void shouldExecuteGatewayWhenExplicitlyAuthorized() {

        System.setProperty(
                "permitirOperacoesComEfeito",
                "true"
        );

        AtomicBoolean procedureCalled =
                new AtomicBoolean(false);

        PurchaseProcedureGateway gateway =
                instruction ->
                        procedureCalled.set(true);

        SqlServerPurchaseExecutor executor =
                new SqlServerPurchaseExecutor(
                        gateway,
                        fixedClock()
                );

        PurchaseExecutionResult result =
                executor.execute(
                        instruction()
                );

        assertTrue(
                procedureCalled.get()
        );

        assertTrue(
                result.isSuccessful()
        );

        assertEquals(
                result.getExecutedAt(),
                Instant.parse(
                        "2026-07-23T10:00:00Z"
                )
        );
    }

    private static PurchaseInstruction instruction() {

        return new PurchaseInstruction(
                new AccountId("10084877"),
                PurchaseOperation.INSTALLMENT,
                new BigDecimal("2828.00"),
                7,
                new BigDecimal("404.00"),
                new BigDecimal("2828.00"),
                LocalDate.of(2026, 7, 23),
                LocalDate.of(2026, 7, 23)
        );
    }

    private static Clock fixedClock() {

        return Clock.fixed(
                Instant.parse(
                        "2026-07-23T10:00:00Z"
                ),
                ZoneOffset.UTC
        );
    }
}