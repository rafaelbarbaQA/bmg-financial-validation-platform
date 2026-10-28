package com.automation.application.purchase.generation;

import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public final class PurchaseInstructionGeneratorTest {

    private static final long SEED =
            20260723L;

    private final PurchaseInstructionGenerator generator =
            new PurchaseInstructionGenerator(
                    SEED,
                    LocalDate.of(
                            2026,
                            7,
                            23
                    )
            );

    @Test
    public void shouldGenerateCashPurchase() {

        PurchaseInstruction instruction =
                generator.generate(
                        new AccountId("11976993"),
                        PurchaseOperation.CASH
                );

        assertEquals(
                instruction.getOperation(),
                PurchaseOperation.CASH
        );

        assertEquals(
                instruction.getInstallmentCount(),
                1
        );

        assertEquals(
                instruction.getPurchaseAmount(),
                instruction.getInstallmentAmount()
        );

        assertEquals(
                instruction.getPurchaseAmount(),
                instruction.getContractAmount()
        );

        assertTrue(
                instruction.getPurchaseAmount()
                        .compareTo(
                                new BigDecimal("50.00")
                        ) >= 0
        );

        assertTrue(
                instruction.getPurchaseAmount()
                        .compareTo(
                                new BigDecimal("2000.00")
                        ) <= 0
        );

        assertDates(
                instruction
        );
    }

    @Test
    public void shouldGenerateValidInstallmentPurchase() {

        PurchaseInstruction instruction =
                generator.generate(
                        new AccountId("11976994"),
                        PurchaseOperation.INSTALLMENT
                );

        assertTrue(
                Arrays.asList(
                        4,
                        7,
                        10
                ).contains(
                        instruction.getInstallmentCount()
                )
        );

        assertTrue(
                instruction.getPurchaseAmount()
                        .compareTo(
                                new BigDecimal("500.00")
                        ) >= 0
        );

        assertTrue(
                instruction.getPurchaseAmount()
                        .compareTo(
                                new BigDecimal("5000.00")
                        ) <= 0
        );

        assertEquals(
                instruction
                        .getInstallmentAmount()
                        .remainder(
                                BigDecimal.ONE
                        ),
                new BigDecimal("0.00")
        );

        BigDecimal expectedContract =
                instruction
                        .getInstallmentAmount()
                        .multiply(
                                BigDecimal.valueOf(
                                        instruction
                                                .getInstallmentCount()
                                )
                        );

        assertEquals(
                instruction.getPurchaseAmount(),
                expectedContract
        );

        assertEquals(
                instruction.getContractAmount(),
                instruction.getPurchaseAmount()
        );

        assertDates(
                instruction
        );
    }

    @Test
    public void shouldBeDeterministicForSameAccountAndSeed() {

        AccountId accountId =
                new AccountId(
                        "11976995"
                );

        PurchaseInstruction first =
                generator.generate(
                        accountId,
                        PurchaseOperation.INSTALLMENT
                );

        PurchaseInstruction second =
                generator.generate(
                        accountId,
                        PurchaseOperation.INSTALLMENT
                );

        assertEquals(
                second.getPurchaseAmount(),
                first.getPurchaseAmount()
        );

        assertEquals(
                second.getInstallmentCount(),
                first.getInstallmentCount()
        );

        assertEquals(
                second.getInstallmentAmount(),
                first.getInstallmentAmount()
        );
    }

    @Test
    public void shouldGenerateRefinancingUsingKnownProfile() {

        PurchaseInstruction instruction =
                generator.generate(
                        new AccountId("11976996"),
                        PurchaseOperation.REFINANCING
                );

        assertEquals(
                instruction.getOperation(),
                PurchaseOperation.REFINANCING
        );

        assertTrue(
                Arrays.asList(
                        3,
                        7,
                        12,
                        24,
                        48
                ).contains(
                        instruction.getInstallmentCount()
                )
        );

        BigDecimal expectedContract =
                instruction
                        .getInstallmentAmount()
                        .multiply(
                                BigDecimal.valueOf(
                                        instruction
                                                .getInstallmentCount()
                                )
                        );

        assertEquals(
                instruction.getContractAmount(),
                expectedContract
        );

        assertTrue(
                instruction.getPurchaseAmount()
                        .compareTo(
                                BigDecimal.ZERO
                        ) > 0
        );

        assertDates(
                instruction
        );
    }
    private static void assertDates(
            PurchaseInstruction instruction
    ) {
        LocalDate expected =
                LocalDate.of(
                        2026,
                        7,
                        23
                );

        assertEquals(
                instruction.getPurchaseDate(),
                expected
        );

        assertEquals(
                instruction.getEntryDate(),
                expected
        );
    }
}