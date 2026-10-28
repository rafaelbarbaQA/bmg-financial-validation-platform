package com.automation.application.purchase.generation;

import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Random;

public final class PurchaseInstructionGenerator {

    private static final int MINIMUM_CASH_AMOUNT = 50;
    private static final int MAXIMUM_CASH_AMOUNT = 2000;

    private static final int MINIMUM_INSTALLMENT_AMOUNT = 500;
    private static final int MAXIMUM_INSTALLMENT_AMOUNT = 5000;

    private static final int[] INSTALLMENT_OPTIONS = {
            4,
            7,
            10
    };

    /*
     * Perfis reais observados para refinanciamento 10376.
     */
    private static final FixedInstallmentProfile[] REFINANCING_PROFILES = {

            new FixedInstallmentProfile(
                    "REF_3X",
                    "179.62",
                    3,
                    "60.41",
                    "181.23"
            ),

            new FixedInstallmentProfile(
                    "REF_7X",
                    "483.56",
                    7,
                    "70.07",
                    "490.49"
            ),

            new FixedInstallmentProfile(
                    "REF_12X",
                    "676.51",
                    12,
                    "67.17",
                    "806.04"
            ),

            new FixedInstallmentProfile(
                    "REF_24X",
                    "1080.30",
                    24,
                    "80.58",
                    "1933.92"
            ),

            new FixedInstallmentProfile(
                    "REF_48X",
                    "3521.71",
                    48,
                    "129.96",
                    "6238.08"
            )
    };

    /*
     * Perfis reais observados para saque parcelado 10436.
     *
     * 5 perfis de 96 parcelas
     * 5 perfis de 84 parcelas
     */
    private static final FixedInstallmentProfile[] CASH_WITHDRAWAL_PROFILES = {

            new FixedInstallmentProfile(
                    "SAQUE_96_01",
                    "158.00",
                    96,
                    "4.39",
                    "421.44"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_96_02",
                    "275.90",
                    96,
                    "7.68",
                    "737.28"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_96_03",
                    "408.00",
                    96,
                    "11.40",
                    "1094.40"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_96_04",
                    "503.12",
                    96,
                    "14.18",
                    "1361.28"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_96_05",
                    "645.71",
                    96,
                    "18.04",
                    "1731.84"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_84_01",
                    "254.12",
                    84,
                    "7.51",
                    "630.84"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_84_02",
                    "432.00",
                    84,
                    "12.46",
                    "1046.64"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_84_03",
                    "715.56",
                    84,
                    "20.94",
                    "1758.96"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_84_04",
                    "1326.00",
                    84,
                    "38.32",
                    "3218.88"
            ),

            new FixedInstallmentProfile(
                    "SAQUE_84_05",
                    "1518.52",
                    84,
                    "44.62",
                    "3748.08"
            )
    };

    private final long seed;
    private final LocalDate movementDate;

    public PurchaseInstructionGenerator(
            long seed,
            LocalDate movementDate
    ) {
        this.seed = seed;

        this.movementDate =
                Objects.requireNonNull(
                        movementDate,
                        "Movement date cannot be null."
                );
    }

    public PurchaseInstruction generate(
            AccountId accountId,
            PurchaseOperation operation
    ) {
        Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        Objects.requireNonNull(
                operation,
                "Purchase operation cannot be null."
        );

        if (operation == PurchaseOperation.CASH) {
            return generateCash(
                    accountId
            );
        }

        if (operation == PurchaseOperation.INSTALLMENT) {
            return generateInstallment(
                    accountId
            );
        }

        if (operation == PurchaseOperation.REFINANCING) {
            return generateFromProfile(
                    accountId,
                    operation,
                    REFINANCING_PROFILES
            );
        }

        if (operation
                == PurchaseOperation.INSTALLMENT_CASH_WITHDRAWAL) {

            return generateFromProfile(
                    accountId,
                    operation,
                    CASH_WITHDRAWAL_PROFILES
            );
        }

        throw new IllegalArgumentException(
                "Unsupported purchase operation: "
                        + operation
        );
    }

    private PurchaseInstruction generateCash(
            AccountId accountId
    ) {
        Random random =
                randomFor(
                        accountId,
                        PurchaseOperation.CASH
                );

        int amountValue =
                nextIntInclusive(
                        random,
                        MINIMUM_CASH_AMOUNT,
                        MAXIMUM_CASH_AMOUNT
                );

        BigDecimal amount =
                money(
                        amountValue
                );

        return new PurchaseInstruction(
                accountId,
                PurchaseOperation.CASH,
                amount,
                1,
                amount,
                amount,
                movementDate,
                movementDate
        );
    }

    private PurchaseInstruction generateInstallment(
            AccountId accountId
    ) {
        Random random =
                randomFor(
                        accountId,
                        PurchaseOperation.INSTALLMENT
                );

        int installmentCount =
                INSTALLMENT_OPTIONS[
                        random.nextInt(
                                INSTALLMENT_OPTIONS.length
                        )
                ];

        int minimumMultiple =
                divideRoundUp(
                        MINIMUM_INSTALLMENT_AMOUNT,
                        installmentCount
                );

        int maximumMultiple =
                MAXIMUM_INSTALLMENT_AMOUNT
                        / installmentCount;

        int installmentAmountValue =
                nextIntInclusive(
                        random,
                        minimumMultiple,
                        maximumMultiple
                );

        int purchaseAmountValue =
                installmentAmountValue
                        * installmentCount;

        BigDecimal purchaseAmount =
                money(
                        purchaseAmountValue
                );

        BigDecimal installmentAmount =
                money(
                        installmentAmountValue
                );

        return new PurchaseInstruction(
                accountId,
                PurchaseOperation.INSTALLMENT,
                purchaseAmount,
                installmentCount,
                installmentAmount,
                purchaseAmount,
                movementDate,
                movementDate
        );
    }

    private PurchaseInstruction generateFromProfile(
            AccountId accountId,
            PurchaseOperation operation,
            FixedInstallmentProfile[] profiles
    ) {
        Random random =
                randomFor(
                        accountId,
                        operation
                );

        FixedInstallmentProfile profile =
                profiles[
                        random.nextInt(
                                profiles.length
                        )
                ];

        return new PurchaseInstruction(
                accountId,
                operation,
                profile.purchaseAmount,
                profile.installmentCount,
                profile.installmentAmount,
                profile.contractAmount,
                movementDate,
                movementDate
        );
    }

    private Random randomFor(
            AccountId accountId,
            PurchaseOperation operation
    ) {
        long accountHash =
                accountId
                        .getValue()
                        .hashCode();

        long operationHash =
                operation
                        .getOperationId();

        return new Random(
                seed
                        ^ accountHash
                        ^ (operationHash << 32)
        );
    }

    private static int divideRoundUp(
            int value,
            int divisor
    ) {
        return (value + divisor - 1)
                / divisor;
    }

    private static int nextIntInclusive(
            Random random,
            int minimum,
            int maximum
    ) {
        return minimum
                + random.nextInt(
                        maximum - minimum + 1
                );
    }

    private static BigDecimal money(
            int value
    ) {
        return BigDecimal
                .valueOf(value)
                .setScale(2);
    }

    private static final class FixedInstallmentProfile {

        private final String name;
        private final BigDecimal purchaseAmount;
        private final int installmentCount;
        private final BigDecimal installmentAmount;
        private final BigDecimal contractAmount;

        private FixedInstallmentProfile(
                String name,
                String purchaseAmount,
                int installmentCount,
                String installmentAmount,
                String contractAmount
        ) {
            this.name =
                    Objects.requireNonNull(
                            name,
                            "Profile name cannot be null."
                    );

            this.purchaseAmount =
                    new BigDecimal(
                            purchaseAmount
                    ).setScale(2);

            this.installmentCount =
                    installmentCount;

            this.installmentAmount =
                    new BigDecimal(
                            installmentAmount
                    ).setScale(2);

            this.contractAmount =
                    new BigDecimal(
                            contractAmount
                    ).setScale(2);

            validateContractAmount();
        }

        private void validateContractAmount() {

            BigDecimal calculatedContract =
                    installmentAmount
                            .multiply(
                                    BigDecimal.valueOf(
                                            installmentCount
                                    )
                            )
                            .setScale(2);

            if (calculatedContract.compareTo(
                    contractAmount
            ) != 0) {

                throw new IllegalArgumentException(
                        "Invalid fixed profile "
                                + name
                                + ". Expected contract "
                                + calculatedContract
                                + " but received "
                                + contractAmount
                                + "."
                );
            }
        }
    }
}