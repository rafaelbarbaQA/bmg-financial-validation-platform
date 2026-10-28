package com.automation.domain.purchase;

import com.automation.domain.account.AccountId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class PurchaseInstruction {

    private final AccountId accountId;
    private final PurchaseOperation operation;
    private final BigDecimal purchaseAmount;
    private final int installmentCount;
    private final BigDecimal installmentAmount;
    private final BigDecimal contractAmount;
    private final LocalDate purchaseDate;
    private final LocalDate entryDate;

    public PurchaseInstruction(
            AccountId accountId,
            PurchaseOperation operation,
            BigDecimal purchaseAmount,
            int installmentCount,
            BigDecimal installmentAmount,
            BigDecimal contractAmount,
            LocalDate purchaseDate,
            LocalDate entryDate
    ) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.operation = Objects.requireNonNull(
                operation,
                "Purchase operation cannot be null."
        );

        this.purchaseAmount =
                requirePositive(
                        purchaseAmount,
                        "Purchase amount"
                );

        if (installmentCount <= 0) {
            throw new IllegalArgumentException(
                    "Installment count must be greater than zero."
            );
        }

        this.installmentCount = installmentCount;

        this.installmentAmount =
                requirePositive(
                        installmentAmount,
                        "Installment amount"
                );

        this.contractAmount =
                requirePositive(
                        contractAmount,
                        "Contract amount"
                );

        this.purchaseDate = Objects.requireNonNull(
                purchaseDate,
                "Purchase date cannot be null."
        );

        this.entryDate = Objects.requireNonNull(
                entryDate,
                "Entry date cannot be null."
        );
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public PurchaseOperation getOperation() {
        return operation;
    }

    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    public int getInstallmentCount() {
        return installmentCount;
    }

    public BigDecimal getInstallmentAmount() {
        return installmentAmount;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    private static BigDecimal requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be greater than zero."
            );
        }

        return value;
    }
}