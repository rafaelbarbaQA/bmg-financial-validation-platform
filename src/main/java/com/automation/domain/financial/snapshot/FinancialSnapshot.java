package com.automation.domain.financial.snapshot;

import com.automation.domain.account.AccountId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public final class FinancialSnapshot {

    private final AccountId accountId;
    private final int accountStatus;

    private final String historyId;
    private final String ticketId;
    private final String nossoNumero;

    private final BigDecimal currentBalance;
    private final BigDecimal minimumAmount;
    private final BigDecimal invoiceAmount;

    private final LocalDate previewDate;
    private final LocalDate expectedCutoffDate;
    private final LocalDate dueDate;

    private final Instant capturedAt;

    public FinancialSnapshot(
            AccountId accountId,
            int accountStatus,
            String historyId,
            String ticketId,
            String nossoNumero,
            BigDecimal currentBalance,
            BigDecimal minimumAmount,
            BigDecimal invoiceAmount,
            LocalDate previewDate,
            LocalDate expectedCutoffDate,
            LocalDate dueDate,
            Instant capturedAt
    ) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.accountStatus = requireValidStatus(accountStatus);

        this.historyId = requireText(
                historyId,
                "History ID"
        );

        this.ticketId = requireText(
                ticketId,
                "Ticket ID"
        );

        this.nossoNumero = requireText(
                nossoNumero,
                "NossoNumero"
        );

        this.currentBalance = requireNonNegativeAmount(
                currentBalance,
                "Current balance"
        );

        this.minimumAmount = requireNonNegativeAmount(
                minimumAmount,
                "Minimum amount"
        );

        this.invoiceAmount = requireNonNegativeAmount(
                invoiceAmount,
                "Invoice amount"
        );

        this.previewDate = previewDate;

        this.expectedCutoffDate = Objects.requireNonNull(
                expectedCutoffDate,
                "Expected cutoff date cannot be null."
        );

        this.dueDate = Objects.requireNonNull(
                dueDate,
                "Due date cannot be null."
        );

        this.capturedAt = Objects.requireNonNull(
                capturedAt,
                "Snapshot capture date cannot be null."
        );

        validateAmounts();
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public int getAccountStatus() {
        return accountStatus;
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getNossoNumero() {
        return nossoNumero;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getMinimumAmount() {
        return minimumAmount;
    }

    public BigDecimal getInvoiceAmount() {
        return invoiceAmount;
    }

    public LocalDate getPreviewDate() {
        return previewDate;
    }

    public LocalDate getExpectedCutoffDate() {
        return expectedCutoffDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    private void validateAmounts() {
        if (minimumAmount.compareTo(invoiceAmount) > 0) {
            throw new IllegalArgumentException(
                    "Minimum amount cannot be greater than invoice amount."
            );
        }
    }

    private static int requireValidStatus(
            int status
    ) {
        if (status < 0) {
            throw new IllegalArgumentException(
                    "Account status cannot be negative."
            );
        }

        return status;
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

    private static BigDecimal requireNonNegativeAmount(
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
}