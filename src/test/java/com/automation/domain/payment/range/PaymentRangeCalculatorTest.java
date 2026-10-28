package com.automation.domain.payment.range;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class PaymentRangeCalculatorTest {

    private PaymentRangeCalculator calculator;

    @BeforeMethod
    public void setUp() {
        calculator = new PaymentRangeCalculator();
    }

    @Test
    public void shouldCalculatePaymentBelowMinimum() {
        PaymentRange range = new PaymentRange(
                BigDecimal.ZERO,
                new BigDecimal("50.00")
        );

        BigDecimal result = calculator.calculate(
                range,
                RangePercentage.of("50")
        );

        assertEquals(
                result,
                new BigDecimal("25.00")
        );
    }

    @Test
    public void shouldCalculateTwentyPercentBetweenMinimumAndTotal() {
        PaymentRange range = new PaymentRange(
                new BigDecimal("200.00"),
                new BigDecimal("250.00")
        );

        BigDecimal result = calculator.calculate(
                range,
                RangePercentage.of("20")
        );

        assertEquals(
                result,
                new BigDecimal("210.00")
        );
    }

    @Test
    public void shouldCalculateSixtyPercentBetweenMinimumAndTotal() {
        PaymentRange range = new PaymentRange(
                new BigDecimal("200.00"),
                new BigDecimal("250.00")
        );

        BigDecimal result = calculator.calculate(
                range,
                RangePercentage.of("60")
        );

        assertEquals(
                result,
                new BigDecimal("230.00")
        );
    }

    @Test
    public void shouldRoundMonetaryResult() {
        PaymentRange range = new PaymentRange(
                new BigDecimal("10.00"),
                new BigDecimal("10.03")
        );

        BigDecimal result = calculator.calculate(
                range,
                RangePercentage.of("50")
        );

        assertEquals(
                result,
                new BigDecimal("10.02")
        );
    }

    @Test
    public void shouldRejectNullRange() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> calculator.calculate(
                        null,
                        RangePercentage.of("50")
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment range cannot be null."
        );
    }

    @Test
    public void shouldRejectNullPercentage() {
        PaymentRange range = new PaymentRange(
                BigDecimal.ZERO,
                new BigDecimal("50.00")
        );

        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> calculator.calculate(
                        range,
                        null
                )
        );

        assertEquals(
                exception.getMessage(),
                "Range percentage cannot be null."
        );
    }
}