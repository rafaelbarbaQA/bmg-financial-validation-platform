package com.automation.application.financial.prebatch;

import com.automation.application.purchase.execution.PurchaseExecutionCoordinator;
import com.automation.application.purchase.execution.PurchaseExecutionResult;
import com.automation.application.purchase.execution.PurchaseExecutor;
import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class FinancialPreBatchPurchaseServiceTest {

    @Test
    public void shouldExecuteCashPurchaseAsPreBatchMovement() {

        AtomicReference<PurchaseInstruction> capturedInstruction =
                new AtomicReference<PurchaseInstruction>();

        PurchaseExecutor executor =
                instruction -> {

                    capturedInstruction.set(
                            instruction
                    );

                    return PurchaseExecutionResult.success(
                            instruction,
                            "Fake PRE-BATCH purchase.",
                            Instant.parse(
                                    "2026-07-23T10:00:00Z"
                            )
                    );
                };

        FinancialPreBatchPurchaseService service =
                new FinancialPreBatchPurchaseService(
                        new PurchaseInstructionGenerator(
                                20260723L,
                                LocalDate.of(
                                        2026,
                                        7,
                                        23
                                )
                        ),
                        new PurchaseExecutionCoordinator(
                                executor
                        )
                );

        PurchaseExecutionResult result =
                service.execute(
                        new AccountId("11976993"),
                        PurchaseOperation.CASH
                );

        assertTrue(
                result.isSuccessful()
        );

        assertNotNull(
                capturedInstruction.get()
        );

        assertEquals(
                capturedInstruction
                        .get()
                        .getOperation(),
                PurchaseOperation.CASH
        );

        assertEquals(
                capturedInstruction
                        .get()
                        .getInstallmentCount(),
                1
        );
    }

    @Test
    public void shouldExecuteInstallmentPurchaseAsPreBatchMovement() {

        PurchaseExecutor executor =
                instruction ->
                        PurchaseExecutionResult.success(
                                instruction,
                                "Fake PRE-BATCH installment purchase.",
                                Instant.parse(
                                        "2026-07-23T10:00:00Z"
                                )
                        );

        FinancialPreBatchPurchaseService service =
                new FinancialPreBatchPurchaseService(
                        new PurchaseInstructionGenerator(
                                20260723L,
                                LocalDate.of(
                                        2026,
                                        7,
                                        23
                                )
                        ),
                        new PurchaseExecutionCoordinator(
                                executor
                        )
                );

        PurchaseExecutionResult result =
                service.execute(
                        new AccountId("11976994"),
                        PurchaseOperation.INSTALLMENT
                );

        PurchaseInstruction instruction =
                result.getInstruction();

        assertTrue(
                result.isSuccessful()
        );

        assertEquals(
                instruction.getOperation(),
                PurchaseOperation.INSTALLMENT
        );

        assertTrue(
                instruction.getInstallmentCount() == 4
                        || instruction.getInstallmentCount() == 7
                        || instruction.getInstallmentCount() == 10
        );

        assertEquals(
                instruction.getPurchaseAmount(),
                instruction.getContractAmount()
        );
    }
}