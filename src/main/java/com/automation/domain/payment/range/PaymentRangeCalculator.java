package com.automation.domain.payment.range;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PaymentRangeCalculator {

    private static final int MONETARY_SCALE = 2;

    public BigDecimal calculate(
            PaymentRange range,
            RangePercentage percentage
    ) {
        Objects.requireNonNull(
                range,
                "Payment range cannot be null."
        );

        Objects.requireNonNull(
                percentage,
                "Range percentage cannot be null."
        );

        BigDecimal percentageAmount = range.difference()
                .multiply(percentage.asDecimalFactor());

        return range.getLowerLimit()
                .add(percentageAmount)
                .setScale(
                        MONETARY_SCALE,
                        RoundingMode.HALF_UP
                );
    }
}