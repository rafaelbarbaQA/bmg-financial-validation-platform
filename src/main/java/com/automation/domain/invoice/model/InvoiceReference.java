package com.automation.domain.invoice.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class InvoiceReference {

    private final String ticketId;
    private final String nossoNumero;
    private final BigDecimal totalAmount;
    private final BigDecimal minimumAmount;

    public InvoiceReference(
            String ticketId,
            String nossoNumero,
            BigDecimal totalAmount,
            BigDecimal minimumAmount
    ) {
        this.ticketId = requireText(
                ticketId,
                "Ticket ID"
        );

        this.nossoNumero = requireText(
                nossoNumero,
                "Nosso número"
        );

        this.totalAmount = requirePositiveAmount(
                totalAmount,
                "Invoice total amount"
        );

        this.minimumAmount = requirePositiveAmount(
                minimumAmount,
                "Invoice minimum amount"
        );

        validateAmounts();
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getNossoNumero() {
        return nossoNumero;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    private void validateAmounts() {
        if (minimumAmount.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException(
                    "Invoice minimum amount cannot be greater than total amount."
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null."
        );

        String normalizedValue = value.trim();

        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return normalizedValue;
    }

    private static BigDecimal requirePositiveAmount(
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