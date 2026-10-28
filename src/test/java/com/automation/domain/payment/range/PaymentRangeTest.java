package com.automation.domain.payment.range;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class PaymentRangeTest {

    @Test
    public void shouldCreatePaymentRange() {
        PaymentRange range = new PaymentRange(
                new BigDecimal("200.00"),
                new BigDecimal("250.00")
        );

        assertEquals(
                range.getLowerLimit(),
                new BigDecimal("200.00")
        );

        assertEquals(
                range.getUpperLimit(),
                new BigDecimal("250.00")
        );

        assertEquals(
                range.difference(),
                new BigDecimal("50.00")
        );
    }

    @Test
    public void shouldAcceptZeroAsLowerLimit() {
        PaymentRange range = new PaymentRange(
                BigDecimal.ZERO,
                new BigDecimal("50.00")
        );

        assertEquals(
                range.difference(),
                new BigDecimal("50.00")
        );
    }

    @Test
    public void shouldRejectNegativeLowerLimit() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new PaymentRange(
                        new BigDecimal("-0.01"),
                        new BigDecimal("50.00")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment range lower limit cannot be negative."
        );
    }

    @Test
    public void shouldRejectZeroUpperLimit() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new PaymentRange(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment range upper limit must be greater than zero."
        );
    }

    @Test
    public void shouldRejectEqualLimits() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new PaymentRange(
                        new BigDecimal("50.00"),
                        new BigDecimal("50.00")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment range lower limit must be lower than upper limit."
        );
    }

    @Test
    public void shouldRejectLowerLimitGreaterThanUpperLimit() {
        IllegalArgumentException exception = expectThrows(
                IllegalArgumentException.class,
                () -> new PaymentRange(
                        new BigDecimal("60.00"),
                        new BigDecimal("50.00")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment range lower limit must be lower than upper limit."
        );
    }
}