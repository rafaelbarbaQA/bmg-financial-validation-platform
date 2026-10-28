package com.automation.domain.payment.range;

import java.math.BigDecimal;
import java.util.Objects;

public final class RangePercentage {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final BigDecimal value;

    public RangePercentage(BigDecimal value) {
        this.value = requireValidPercentage(value);
    }

    public static RangePercentage of(String value) {
        return new RangePercentage(
                new BigDecimal(value)
        );
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal asDecimalFactor() {
        return value.divide(ONE_HUNDRED);
    }

    private static BigDecimal requireValidPercentage(
            BigDecimal value
    ) {
        Objects.requireNonNull(
                value,
                "Range percentage cannot be null."
        );

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Range percentage must be greater than zero."
            );
        }

        if (value.compareTo(ONE_HUNDRED) >= 0) {
            throw new IllegalArgumentException(
                    "Range percentage must be lower than one hundred."
            );
        }

        return value;
    }
}