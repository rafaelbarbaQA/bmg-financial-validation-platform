package com.automation.application.payment.validation;

import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.repository.PaymentEventDetailsRepository;
import com.automation.domain.payment.model.PaymentEventDetails;

import java.math.BigDecimal;
import java.util.Objects;

public final class PaymentEventValidationService {

    private final PaymentEventDetailsRepository repository;

    public PaymentEventValidationService(
            PaymentEventDetailsRepository repository
    ) {

        this.repository = Objects.requireNonNull(
                repository,
                "Payment event details repository cannot be null."
        );
    }

    public PaymentEventDetails validateInsertedPayment(
            PaymentExecutionResult executionResult,
            String expectedNossoNumero,
            int expectedStatus
    ) {

        Objects.requireNonNull(
                executionResult,
                "Payment execution result cannot be null."
        );

        if (!executionResult.isSuccessful()) {

            throw new IllegalStateException(
                    "A failed payment execution cannot be validated in database."
            );
        }

        if (expectedStatus < 0) {

            throw new IllegalArgumentException(
                    "Expected payment status cannot be negative."
            );
        }

        long paymentEventId =
                parsePaymentEventId(
                        executionResult.getExternalPaymentId()
                );

        PaymentEventDetails details =
                repository
                        .findByPaymentEventId(
                                paymentEventId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment event "
                                                + paymentEventId
                                                + " was not found in database."
                                )
                        );

        validateEventId(
                paymentEventId,
                details
        );

        validateAccount(
                executionResult,
                details
        );

        validateAmount(
                executionResult,
                details
        );

        validateNossoNumero(
                expectedNossoNumero,
                details
        );

        validateStatus(
                expectedStatus,
                details
        );

        return details;
    }

    private static void validateEventId(
            long expectedPaymentEventId,
            PaymentEventDetails details
    ) {

        if (details.getPaymentEventId()
                != expectedPaymentEventId) {

            throw new IllegalStateException(
                    "Payment event ID differs from expected. "
                            + "Expected: "
                            + expectedPaymentEventId
                            + ". Actual: "
                            + details.getPaymentEventId()
                            + "."
            );
        }
    }

    private static void validateAccount(
            PaymentExecutionResult executionResult,
            PaymentEventDetails details
    ) {

        String expectedAccount =
                executionResult
                        .getAccountId()
                        .getValue();

        String actualAccount =
                details
                        .getAccountId()
                        .getValue();

        if (!expectedAccount.equals(actualAccount)) {

            throw new IllegalStateException(
                    "Payment account differs from expected. "
                            + "Expected: "
                            + expectedAccount
                            + ". Actual: "
                            + actualAccount
                            + "."
            );
        }
    }

    private static void validateAmount(
            PaymentExecutionResult executionResult,
            PaymentEventDetails details
    ) {

        BigDecimal expectedAmount =
                executionResult
                        .getInstruction()
                        .getAmount();

        BigDecimal actualAmount =
                details.getAmount();

        if (expectedAmount.compareTo(actualAmount) != 0) {

            throw new IllegalStateException(
                    "Payment amount differs from expected. "
                            + "Expected: "
                            + expectedAmount
                            + ". Actual: "
                            + actualAmount
                            + "."
            );
        }
    }

    private static void validateNossoNumero(
            String expectedNossoNumero,
            PaymentEventDetails details
    ) {

        String expected =
                normalizeNumericIdentifier(
                        expectedNossoNumero
                );

        String actual =
                normalizeNumericIdentifier(
                        details.getNossoNumero()
                );

        if (!expected.equals(actual)) {

            throw new IllegalStateException(
                    "NossoNumero differs from expected. "
                            + "Expected: "
                            + expected
                            + ". Actual: "
                            + actual
                            + "."
            );
        }
    }

    private static void validateStatus(
            int expectedStatus,
            PaymentEventDetails details
    ) {

        if (details.getStatus() != expectedStatus) {

            throw new IllegalStateException(
                    "Initial payment status differs from expected. "
                            + "Expected: "
                            + expectedStatus
                            + ". Actual: "
                            + details.getStatus()
                            + "."
            );
        }
    }

    private static long parsePaymentEventId(
            String externalPaymentId
    ) {

        if (externalPaymentId == null
                || externalPaymentId.trim().isEmpty()) {

            throw new IllegalStateException(
                    "External payment ID cannot be empty."
            );
        }

        try {

            long paymentEventId =
                    Long.parseLong(
                            externalPaymentId.trim()
                    );

            if (paymentEventId <= 0) {

                throw new IllegalStateException(
                        "Payment event ID must be greater than zero."
                );
            }

            return paymentEventId;

        } catch (NumberFormatException exception) {

            throw new IllegalStateException(
                    "External payment ID must contain "
                            + "the numeric Id_EventoPagamento. "
                            + "Actual: "
                            + externalPaymentId,
                    exception
            );
        }
    }

    private static String normalizeNumericIdentifier(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Numeric identifier cannot be empty."
            );
        }

        String normalized =
                value.trim();

        try {

            return new BigDecimal(
                    normalized
            )
                    .stripTrailingZeros()
                    .toPlainString();

        } catch (NumberFormatException exception) {

            return normalized;
        }
    }
}