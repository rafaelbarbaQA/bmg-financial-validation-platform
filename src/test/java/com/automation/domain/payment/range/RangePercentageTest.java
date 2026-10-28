package com.automation.domain.payment.range;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class RangePercentageTest {

    @Test
    public void shouldCreatePercentage() {
        RangePercentage percentage =
                RangePercentage.of("20");

        assertEquals(
                percentage.getValue(),
                new BigDecimal("20")
        );

        assertEquals(
                percentage.asDecimalFactor(),
                new BigDecimal("0.2")
        );
    }

    @Test
    public void shouldCreateDecimalPercentage() {
        RangePercentage percentage =
                RangePercentage.of("12.50");

        assertEquals(
                percentage.asDecimalFactor(),
                new BigDecimal("0.125")
        );
    }

    @Test
    public void shouldRejectZeroPercentage() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> RangePercentage.of("0")
        );

        assertEquals(
                exception.getMessage(),
                "Range percentage must be greater than zero."
        );
    }

    @Test
    public void shouldRejectNegativePercentage() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> RangePercentage.of("-1")
        );

        assertEquals(
                exception.getMessage(),
                "Range percentage must be greater than zero."
        );
    }

    @Test
    public void shouldRejectOneHundredPercentage() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> RangePercentage.of("100")
        );

        assertEquals(
                exception.getMessage(),
                "Range percentage must be lower than one hundred."
        );
    }

    @Test
    public void shouldRejectPercentageGreaterThanOneHundred() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> RangePercentage.of("120")
        );

        assertEquals(
                exception.getMessage(),
                "Range percentage must be lower than one hundred."
        );
    }
}