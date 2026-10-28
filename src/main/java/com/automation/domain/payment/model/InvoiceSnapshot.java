package com.automation.domain.payment.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class InvoiceSnapshot {

    private final String accountId;
    private final String invoiceId;
    private final BigDecimal totalAmount;
    private final BigDecimal minimumAmount;

    public InvoiceSnapshot(
            String accountId,
            String invoiceId,
            BigDecimal totalAmount,
            BigDecimal minimumAmount
    ) {
        this.accountId = requireText(accountId, "accountId");
        this.invoiceId = requireText(invoiceId, "invoiceId");
        this.totalAmount = requirePositive(totalAmount, "totalAmount");
        this.minimumAmount = requirePositive(minimumAmount, "minimumAmount");

        validateAmounts();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getInvoiceId() {
        return invoiceId;
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
                    "minimumAmount cannot be greater than totalAmount."
            );
        }
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " cannot be null.");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank."
            );
        }

        return value;
    }

    private static BigDecimal requirePositive(
            BigDecimal value,
            String fieldName
    ) {
        Objects.requireNonNull(value, fieldName + " cannot be null.");

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }

        return value;
    }
}