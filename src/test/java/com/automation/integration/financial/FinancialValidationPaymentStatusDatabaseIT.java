package com.automation.integration.financial;

import com.automation.application.financial.validation.FinancialValidationReport;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.engine.FinancialValidationEngineFactory;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.processing.PaymentProcessingStateService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.FullPaymentScenario;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.PaymentProcessingState;
import com.automation.infrastructure.database.repository.SqlServerPaymentProcessingStateRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public final class FinancialValidationPaymentStatusDatabaseIT {

    @Test
    public void shouldValidateRealPaymentStatusThroughFinancialEngine() {

        long paymentEventId =
                requireLongProperty(
                        "payment.event.id"
                );

        AccountId accountId =
                new AccountId(
                        System.getProperty(
                                "validation.account.id",
                                "1"
                        )
                );

        PaymentInstruction instruction =
                new PaymentInstruction(
                        accountId,
                        PaymentScenario.MINIMUM,
                        new BigDecimal("1.00"),
                        Instant.now()
                );

        PaymentExecutionResult executionResult =
                PaymentExecutionResult.success(
                        instruction,
                        String.valueOf(
                                paymentEventId
                        ),
                        "Existing payment event used by financial validation integration test.",
                        Instant.now()
                );

        PaymentProcessingStateService processingStateService =
                new PaymentProcessingStateService(
                        new SqlServerPaymentProcessingStateRepository()
                );

        PaymentProcessingState processingState =
                processingStateService.load(
                        executionResult
                );

        FinancialSnapshot before =
                createSnapshot(
                        accountId,
                        "1000.00"
                );

        /*
         * O saldo abaixo e propositalmente diferente de zero.
         *
         * Este teste nao valida saldo.
         * Ele valida apenas o status real do pagamento
         * passando pelo FinancialValidationEngine.
         */
        FinancialSnapshot after =
                createSnapshot(
                        accountId,
                        "375.45"
                );

        FinancialValidationContext context =
                new FinancialValidationContext(
                        new FullPaymentScenario(),
                        before,
                        after,
                        new BigDecimal("1000.00"),
                        processingState
                );

        FinancialValidationReport report =
                FinancialValidationEngineFactory
                        .forFullPayment()
                        .validate(context);

        System.out.println();
        System.out.println(
                "=============================================="
        );

        System.out.println(
                " FINANCIAL VALIDATION - REAL DATABASE"
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "Payment Event ID....: "
                        + paymentEventId
        );

        System.out.println(
                "Database Status.....: "
                        + processingState.getStatus()
        );

        System.out.println(
                "Validation Rule.....: "
                        + report.getResults()
                        .get(0)
                        .getRule()
        );

        System.out.println(
                "Expected............: "
                        + report.getResults()
                        .get(0)
                        .getExpected()
        );

        System.out.println(
                "Actual..............: "
                        + report.getResults()
                        .get(0)
                        .getActual()
        );

        System.out.println(
                "Result..............: "
                        + report.getResults()
                        .get(0)
                        .getStatus()
        );

        System.out.println(
                "=============================================="
        );

        assertEquals(
                report.getTotalValidations(),
                1
        );

        assertEquals(
                report.getResults()
                        .get(0)
                        .getRule(),
                "PAYMENT_STATUS"
        );

        assertEquals(
                processingState.getStatus(),
                2
        );

        assertTrue(
                report.passed(),
                "Financial validation should PASS when real payment status is 2."
        );
    }

    private static FinancialSnapshot createSnapshot(
            AccountId accountId,
            String balance
    ) {

        return new FinancialSnapshot(
                accountId,
                0,
                "INTEGRATION-HISTORY",
                "INTEGRATION-TICKET",
                "INTEGRATION-NOSSO-NUMERO",
                new BigDecimal(
                        balance
                ),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(
                        2026,
                        7,
                        5
                ),
                LocalDate.of(
                        2026,
                        7,
                        10
                ),
                LocalDate.of(
                        2026,
                        7,
                        10
                ),
                Instant.now()
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
                || value.trim().isEmpty()) {

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
                                + " deve ser maior que zero."
                );
            }

            return parsed;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    propertyName
                            + " deve ser numerico.",
                    exception
            );
        }
    }
}