package com.automation.domain.account;

import java.util.Objects;

public final class AccountId {

    private final String value;

    public AccountId(String value) {
        this.value = requireValidValue(value);
    }

    public String getValue() {
        return value;
    }

    private static String requireValidValue(String value) {
        Objects.requireNonNull(
                value,
                "Account ID cannot be null."
        );

        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    "Account ID cannot be blank."
            );
        }

        return normalizedValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AccountId)) {
            return false;
        }

        AccountId other = (AccountId) object;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}