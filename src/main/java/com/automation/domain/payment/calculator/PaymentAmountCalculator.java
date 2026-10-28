package com.automation.domain.payment.calculator;

import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.model.InvoiceSnapshot;
import com.automation.domain.payment.range.PaymentRange;
import com.automation.domain.payment.range.PaymentRangeCalculator;
import com.automation.domain.payment.range.RangePercentage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PaymentAmountCalculator {

    private static final RangePercentage BELOW_MINIMUM_PERCENTAGE =
            RangePercentage.of("50");

    private static final RangePercentage PARTIAL_20_PERCENTAGE =
            RangePercentage.of("20");

    private static final RangePercentage PARTIAL_60_PERCENTAGE =
            RangePercentage.of("60");

    private static final BigDecimal ABOVE_TOTAL_PERCENTAGE =
            new BigDecimal("0.20");

    private static final int MONETARY_SCALE = 2;

    private final PaymentRangeCalculator rangeCalculator;

    public PaymentAmountCalculator() {
        this(new PaymentRangeCalculator());
    }

    public PaymentAmountCalculator(
            PaymentRangeCalculator rangeCalculator
    ) {
        this.rangeCalculator = Objects.requireNonNull(
                rangeCalculator,
                "Payment range calculator cannot be null."
        );
    }

    public BigDecimal calculate(
            PaymentScenario scenario,
            InvoiceSnapshot invoice
    ) {
        Objects.requireNonNull(
                scenario,
                "Payment scenario cannot be null."
        );

        Objects.requireNonNull(
                invoice,
                "Invoice snapshot cannot be null."
        );

        switch (scenario) {
            case TOTAL:
                return invoice.getTotalAmount();

            case MINIMUM:
                return invoice.getMinimumAmount();

            case BELOW_MINIMUM:
                return calculateBelowMinimum(invoice);

            case BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT:
                return calculateBetweenMinimumAndTotal(
                        invoice,
                        PARTIAL_20_PERCENTAGE
                );

            case BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT:
                return calculateBetweenMinimumAndTotal(
                        invoice,
                        PARTIAL_60_PERCENTAGE
                );

            case ABOVE_TOTAL:
                return calculateAboveTotal(invoice);

            default:
                throw new IllegalArgumentException(
                        "Unsupported payment scenario: " + scenario
                );
        }
    }

    private BigDecimal calculateBelowMinimum(
            InvoiceSnapshot invoice
    ) {
        PaymentRange range = new PaymentRange(
                BigDecimal.ZERO,
                invoice.getMinimumAmount()
        );

        return rangeCalculator.calculate(
                range,
                BELOW_MINIMUM_PERCENTAGE
        );
    }

    private BigDecimal calculateBetweenMinimumAndTotal(
            InvoiceSnapshot invoice,
            RangePercentage percentage
    ) {
        PaymentRange range = new PaymentRange(
                invoice.getMinimumAmount(),
                invoice.getTotalAmount()
        );

        return rangeCalculator.calculate(
                range,
                percentage
        );
    }

    private BigDecimal calculateAboveTotal(
            InvoiceSnapshot invoice
    ) {
        BigDecimal additionalAmount = invoice.getTotalAmount()
                .multiply(ABOVE_TOTAL_PERCENTAGE);

        return invoice.getTotalAmount()
                .add(additionalAmount)
                .setScale(
                        MONETARY_SCALE,
                        RoundingMode.HALF_UP
                );
    }
}