package com.automation.domain.account.model;

import com.automation.domain.account.AccountId;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.expectThrows;

public final class AccountReferenceTest {

    @Test
    public void shouldCreateAccountReference() {
        AccountId accountId = new AccountId("2310664");

        AccountReference account = new AccountReference(
                accountId,
                596,
                0
        );

        assertSame(account.getAccountId(), accountId);
        assertEquals(account.getProductId(), 596);
        assertEquals(account.getAccountStatus(), 0);
    }

    @Test
    public void shouldRejectNullAccountId() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> new AccountReference(
                        null,
                        596,
                        0
                )
        );

        assertEquals(
                exception.getMessage(),
                "Account ID cannot be null."
        );
    }

    @Test
    public void shouldRejectZeroProductId() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new AccountReference(
                        new AccountId("2310664"),
                        0,
                        0
                )
        );

        assertEquals(
                exception.getMessage(),
                "Product ID must be greater than zero."
        );
    }

    @Test
    public void shouldRejectNegativeAccountStatus() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new AccountReference(
                        new AccountId("2310664"),
                        596,
                        -1
                )
        );

        assertEquals(
                exception.getMessage(),
                "Account status cannot be negative."
        );
    }
}