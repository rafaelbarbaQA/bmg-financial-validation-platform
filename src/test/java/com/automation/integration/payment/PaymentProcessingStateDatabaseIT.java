package com.automation.integration.payment;

import io.qameta.allure.Story;
import io.qameta.allure.Feature;
import io.qameta.allure.Epic;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.processing.PaymentProcessingStateService;
import com.automation.domain.account.AccountId;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.PaymentProcessingState;
import com.automation.infrastructure.database.repository.SqlServerPaymentProcessingStateRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;
@Epic("Financial Validation Platform")
@Feature("Pagamentos")

public final class PaymentProcessingStateDatabaseIT {

    @Test
@Story("Processamento do pagamento")
    public void shouldLoadRealPaymentProcessingStatusFromDatabase() {

        long paymentEventId =
                requireLongProperty(
                        "payment.event.id"
                );

        int expectedStatus =
                Integer.parseInt(
                        System.getProperty(
                                "expected.payment.status",
                                "2"
                        )
                );

        PaymentInstruction instruction =
                new PaymentInstruction(
                        new AccountId("1"),
                        PaymentScenario.MINIMUM,
                        new BigDecimal("1.00"),
                        Instant.now()
                );

        PaymentExecutionResult executionResult =
                PaymentExecutionResult.success(
                        instruction,
                        String.valueOf(paymentEventId),
                        "Existing payment event used for integration validation.",
                        Instant.now()
                );

        PaymentProcessingStateService service =
                new PaymentProcessingStateService(
                        new SqlServerPaymentProcessingStateRepository()
                );

        PaymentProcessingState state =
                service.load(
                        executionResult
                );

        System.out.println(
                "PAYMENT EVENT | id="
                        + paymentEventId
                        + " | expectedStatus="
                        + expectedStatus
                        + " | actualStatus="
                        + state.getStatus()
        );

        assertEquals(
                state.getStatus(),
                expectedStatus,
                "Payment processing status differs from expected value."
        );
    }

    private static long requireLongProperty(
            String propertyName
    ) {

        String value =
                System.getProperty(
                        propertyName
                );

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + propertyName
                            + "=..."
            );
        }

        try {
            long parsed =
                    Long.parseLong(
                            value.trim()
                    );

            if (parsed <= 0) {
                throw new IllegalArgumentException(
                        propertyName
                                + " must be greater than zero."
                );
            }

            return parsed;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    propertyName
                            + " must contain a numeric value.",
                    exception
            );
        }
    }
}
