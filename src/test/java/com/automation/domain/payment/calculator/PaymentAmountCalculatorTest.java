package com.automation.domain.payment.calculator;

import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.model.InvoiceSnapshot;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public final class PaymentAmountCalculatorTest {

    private PaymentAmountCalculator calculator;
    private InvoiceSnapshot invoice;

    @BeforeMethod
    public void setUp() {
        calculator = new PaymentAmountCalculator();

        invoice = new InvoiceSnapshot(
                "2310664",
                "466031434",
                new BigDecimal("1000.00"),
                new BigDecimal("200.00")
        );
    }

    @Test
    public void shouldCalculateTotalPayment() {
        BigDecimal result = calculator.calculate(
                PaymentScenario.TOTAL,
                invoice
        );

        assertEquals(
                result,
                new BigDecimal("1000.00")
        );
    }

    @Test
    public void shouldCalculateMinimumPayment() {
        BigDecimal result = calculator.calculate(
                PaymentScenario.MINIMUM,
                invoice
        );

        assertEquals(
                result,
                new BigDecimal("200.00")
        );
    }

    @Test
    public void shouldCalculatePaymentBelowMinimum() {
        BigDecimal result = calculator.calculate(
                PaymentScenario.BELOW_MINIMUM,
                invoice
        );

        assertEquals(
                result,
                new BigDecimal("100.00")
        );
    }

    @Test
    public void shouldCalculateTwentyPercentBetweenMinimumAndTotal() {
        BigDecimal result = calculator.calculate(
                PaymentScenario.BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT,
                invoice
        );

        assertEquals(
                result,
                new BigDecimal("360.00")
        );
    }

    @Test
    public void shouldCalculateSixtyPercentBetweenMinimumAndTotal() {
        BigDecimal result = calculator.calculate(
                PaymentScenario.BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT,
                invoice
        );

        assertEquals(
                result,
                new BigDecimal("680.00")
        );
    }

    @Test
    public void shouldCalculatePaymentAboveTotal() {
        BigDecimal result = calculator.calculate(
                PaymentScenario.ABOVE_TOTAL,
                invoice
        );

        assertEquals(
                result,
                new BigDecimal("1200.00")
        );
    }

    @Test
    public void shouldCalculateValuesForSmallInterval() {
        InvoiceSnapshot smallIntervalInvoice =
                new InvoiceSnapshot(
                        "2310665",
                        "466031435",
                        new BigDecimal("250.00"),
                        new BigDecimal("200.00")
                );

        BigDecimal twentyPercent = calculator.calculate(
                PaymentScenario.BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT,
                smallIntervalInvoice
        );

        BigDecimal sixtyPercent = calculator.calculate(
                PaymentScenario.BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT,
                smallIntervalInvoice
        );

        assertEquals(
                twentyPercent,
                new BigDecimal("210.00")
        );

        assertEquals(
                sixtyPercent,
                new BigDecimal("230.00")
        );
    }

    @Test
    public void shouldRejectNullScenario() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> calculator.calculate(
                        null,
                        invoice
                )
        );

        assertEquals(
                exception.getMessage(),
                "Payment scenario cannot be null."
        );
    }

    @Test
    public void shouldRejectNullInvoice() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> calculator.calculate(
                        PaymentScenario.TOTAL,
                        null
                )
        );

        assertEquals(
                exception.getMessage(),
                "Invoice snapshot cannot be null."
        );
    }

    @Test
    public void shouldRejectNullRangeCalculator() {
        NullPointerException exception = expectThrows(
                NullPointerException.class,
                () -> new PaymentAmountCalculator(null)
        );

        assertEquals(
                exception.getMessage(),
                "Payment range calculator cannot be null."
        );
    }
}