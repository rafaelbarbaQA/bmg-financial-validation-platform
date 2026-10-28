package com.automation.application.purchase.execution;

import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertTrue;

public final class PurchaseExecutionCoordinatorTest {

    @Test
    public void shouldExecutePurchaseUsingInjectedExecutor() {

        PurchaseInstruction instruction =
                new PurchaseInstruction(
                        new AccountId("11976993"),
                        PurchaseOperation.CASH,
                        new BigDecimal("1000.00"),
                        1,
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        LocalDate.of(2026, 7, 23),
                        LocalDate.of(2026, 7, 23)
                );

        PurchaseExecutor executor =
                purchase ->
                        PurchaseExecutionResult.success(
                                purchase,
                                "Preview purchase executed.",
                                Instant.parse(
                                        "2026-07-23T10:00:00Z"
                                )
                        );

        PurchaseExecutionCoordinator coordinator =
                new PurchaseExecutionCoordinator(
                        executor
                );

        PurchaseExecutionResult result =
                coordinator.execute(
                        instruction
                );

        assertTrue(
                result.isSuccessful()
        );
    }
}