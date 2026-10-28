package com.automation.infrastructure.purchase.execution;

import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class PreviewPurchaseExecutorTest {

    @Test
    public void shouldExecutePurchaseWithoutDatabaseOperation() {

        Instant fixedInstant =
                Instant.parse(
                        "2026-07-23T10:00:00Z"
                );

        PreviewPurchaseExecutor executor =
                new PreviewPurchaseExecutor(
                        Clock.fixed(
                                fixedInstant,
                                ZoneOffset.UTC
                        )
                );

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

        PurchaseExecutionResult result =
                executor.execute(
                        instruction
                );

        assertTrue(
                result.isSuccessful()
        );

        assertEquals(
                result.getInstruction(),
                instruction
        );

        assertEquals(
                result.getExecutedAt(),
                fixedInstant
        );

        assertEquals(
                result.getMessage(),
                "Purchase PREVIEW executed. No database operation was performed."
        );
    }
}