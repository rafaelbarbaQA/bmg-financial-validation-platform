package com.automation.domain.payment.range;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentRange {

    private final BigDecimal lowerLimit;
    private final BigDecimal upperLimit;

    public PaymentRange(
            BigDecimal lowerLimit,
            BigDecimal upperLimit
    ) {
        this.lowerLimit = requireNonNegative(
                lowerLimit,
                "Payment range lower limit"
        );

        this.upperLimit = requirePositive(
                upperLimit,
                "Payment range upper limit"
        );

        validateLimits();
    }

    public BigDecimal getLowerLimit() {
        return lowerLimit;
    }

    public BigDecimal getUpperLimit() {
        return upperLimit;
    }

    public BigDecimal difference() {
        return upperLimit.subtract(lowerLimit);
    }

    private void validateLimits() {
        if (lowerLimit.compareTo(upperLimit) >= 0) {
            throw new IllegalArgumentException(
                    "Payment range lower limit must be lower than upper limit."
            );
        }
    }

    private static BigDecimal requireNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative."
            );
        }

        return value;
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
                    fieldName + " must be greater than zero."
            );
        }

        return value;
    }
}