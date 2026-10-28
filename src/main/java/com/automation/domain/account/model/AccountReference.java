package com.automation.domain.account.model;

import com.automation.domain.account.AccountId;

import java.util.Objects;

public final class AccountReference {

    private final AccountId accountId;
    private final int productId;
    private final int accountStatus;

    public AccountReference(
            AccountId accountId,
            int productId,
            int accountStatus
    ) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.productId = requirePositiveProductId(productId);
        this.accountStatus = requireValidStatus(accountStatus);
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public int getProductId() {
        return productId;
    }

    public int getAccountStatus() {
        return accountStatus;
    }

    private static int requirePositiveProductId(int productId) {
        if (productId <= 0) {
            throw new IllegalArgumentException(
                    "Product ID must be greater than zero."
            );
        }

        return productId;
    }

    private static int requireValidStatus(int accountStatus) {
        if (accountStatus < 0) {
            throw new IllegalArgumentException(
                    "Account status cannot be negative."
            );
        }

        return accountStatus;
    }
}