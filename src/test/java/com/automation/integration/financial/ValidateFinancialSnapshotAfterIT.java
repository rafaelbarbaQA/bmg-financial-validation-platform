package com.automation.integration.financial;

import io.qameta.allure.Story;
import io.qameta.allure.Feature;
import io.qameta.allure.Epic;
import com.automation.application.financial.comparison.FinancialSnapshotComparison;
import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.financial.validation.FinancialValidationReport;
import com.automation.application.financial.validation.context.FinancialValidationContext;
import com.automation.application.financial.validation.engine.FinancialValidationEngine;
import com.automation.application.financial.validation.engine.FinancialValidationEngineFactory;
import com.automation.application.payment.assertion.PaymentPostBatchAssertions;
import com.automation.application.payment.processing.PaymentPostBatchReadinessGuard;
import com.automation.application.payment.processing.PaymentProcessingReadiness;
import com.automation.domain.payment.model.PaymentProcessingState;
import com.automation.application.payment.validation.PaymentPostBatchValidationResult;
import com.automation.application.payment.validation.PaymentPostBatchValidator;
import com.automation.application.payment.validation.PaymentValidationPlan;
import com.automation.application.payment.validation.PaymentValidationPlanFactory;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.FullPaymentScenario;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.PaymentEventDetails;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.database.repository.SqlServerPaymentEventDetailsRepository;
import com.automation.infrastructure.financial.snapshot.FinancialSnapshotFileStore;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.testng.Assert.assertEquals;
@Epic("Financial Validation Platform")
@Feature("Ciclo financeiro")

public final class ValidateFinancialSnapshotAfterIT {

    @Test
@Story("Validacao pos-batch")
    public void shouldValidateRealBeforeAndAfterState() {

        AccountId accountId =
                new AccountId(
                        requireProperty(
                                "validation.account.id"
                        )
                );

        long paymentEventId =
                Long.parseLong(
                        requireProperty(
                                "payment.event.id"
                        )
                );

        BigDecimal expectedPaymentAmount =
                new BigDecimal(
                        requireProperty(
                                "payment.amount"
                        )
                );

        PaymentScenario paymentScenario =
                resolveScenario(
                        requireProperty(
                                "financial.scenario"
                        )
                );

        int expectedAccountStatus =
                Integer.parseInt(
                        System.getProperty(
                                "expected.account.status",
                                "0"
                        )
                );

        // =====================================================
        // 1. PRE-BATCH BASELINE
        // =====================================================

        FinancialSnapshotFileStore store =
                new FinancialSnapshotFileStore();

        FinancialSnapshot before =
                store.loadBefore(
                        accountId
                );

        // =====================================================
        // 2. CURRENT / AFTER SNAPSHOT
        // =====================================================

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot after =
                snapshotService.capture(
                        accountId
                );

        FinancialSnapshotComparison comparison =
                new FinancialSnapshotComparison(
                        before,
                        after
                );

        // =====================================================
        // 3. REAL PAYMENT EVENT
        // =====================================================

        PaymentEventDetails payment =
                new SqlServerPaymentEventDetailsRepository()
                        .findByPaymentEventId(
                                paymentEventId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment event not found: "
                                                + paymentEventId
                                )
                        );

        validatePaymentLink(
                payment,
                accountId,
                expectedPaymentAmount,
                before.getNossoNumero()
        );
        // =====================================================
        // PAYMENT PROCESSING READINESS
        // =====================================================

        PaymentProcessingState processingState =
                new PaymentProcessingState(
                        payment.getStatus()
                );

        PaymentPostBatchReadinessGuard readinessGuard =
                new PaymentPostBatchReadinessGuard();

        PaymentProcessingReadiness readiness =
                readinessGuard.evaluate(
                        processingState
                );

        System.out.println(
                "Payment readiness..........: "
                        + readiness
        );

        readinessGuard.requireReady(
                processingState
        );

        // =====================================================
        // 4. REBUILD ORIGINAL VALIDATION PLAN
        // =====================================================

        PaymentInstruction instruction =
                new PaymentInstruction(
                        accountId,
                        paymentScenario,
                        payment.getAmount(),
                        Instant.now()
                );

        PaymentValidationPlan validationPlan =
                new PaymentValidationPlanFactory()
                        .create(
                                before,
                                instruction
                        );

        // =====================================================
        // 5. POST-BATCH VALIDATION
        // =====================================================

        PaymentPostBatchValidationResult result =
                new PaymentPostBatchValidator()
                        .validate(
                                validationPlan,
                                after
                        );

        assertScenarioResult(
                paymentScenario,
                result
        );

        /*
         * Financial Validation Engine
         *
         * Ativado inicialmente somente para TOTAL,
         * pois a regra de saldo implementada pertence
         * ao pagamento integral.
         */
        if (paymentScenario == PaymentScenario.TOTAL) {

            PaymentProcessingState engineProcessingState =
                    new PaymentProcessingState(
                            payment.getStatus()
                    );

            FinancialValidationContext financialContext =
                    new FinancialValidationContext(
                            new FullPaymentScenario(),
                            before,
                            after,
                            payment.getAmount(),
                            engineProcessingState
                    );

            FinancialValidationEngine financialEngine =
                    FinancialValidationEngineFactory
                            .forFullPaymentWithBalance(
                                    expectedAccountStatus
                            );

            FinancialValidationReport financialReport =
                    financialEngine.validate(
                            financialContext
                    );

            System.out.println();
            System.out.println(
                    "===== FINANCIAL VALIDATION ENGINE ====="
            );

            financialReport.getResults().forEach(
                    validation ->
                            System.out.println(
                                    validation.getRule()
                                            + " -> "
                                            + validation.getStatus()
                                            + " | esperado="
                                            + validation.getExpected()
                                            + " | atual="
                                            + validation.getActual()
                            )
            );

            System.out.println(
                    "ENGINE RESULT.............: "
                            + (
                            financialReport.passed()
                                    ? "PASS"
                                    : "FAIL"
                    )
            );

            assertEquals(
                    financialReport.getTotalValidations(),
                    3,
                    "TOTAL must execute PAYMENT_STATUS, BALANCE and ACCOUNT_STATUS."
            );

            assertEquals(
                    financialReport.passed(),
                    true,
                    "Financial Validation Engine failed for TOTAL payment."
            );
        }

        assertEquals(
                after.getAccountStatus(),
                expectedAccountStatus,
                "Unexpected post-batch account status."
        );

        // =====================================================
        // 6. OUTPUT
        // =====================================================

        System.out.println();
        System.out.println(
                "================ FINANCIAL POST-BATCH VALIDATION ================"
        );

        System.out.println(
                "Conta.....................: "
                        + accountId.getValue()
        );

        System.out.println(
                "Cenario...................: "
                        + paymentScenario
        );

        System.out.println(
                "Evento pagamento..........: "
                        + payment.getPaymentEventId()
        );

        System.out.println(
                "Status pagamento..........: "
                        + payment.getStatus()
        );

        System.out.println(
                "Valor pagamento...........: "
                        + payment.getAmount()
        );

        System.out.println(
                "NossoNumero...............: "
                        + payment.getNossoNumero()
        );

        System.out.println();

        System.out.println(
                "Saldo PRE-BATCH...........: "
                        + before.getCurrentBalance()
        );

        System.out.println(
                "Saldo AFTER...............: "
                        + after.getCurrentBalance()
        );

        System.out.println(
                "Diferenca saldo...........: "
                        + comparison.balanceDifference()
        );

        System.out.println(
                "Saldo mudou?..............: "
                        + comparison.balanceChanged()
        );

        System.out.println();

        System.out.println(
                "Status conta PRE-BATCH....: "
                        + before.getAccountStatus()
        );

        System.out.println(
                "Status conta AFTER........: "
                        + after.getAccountStatus()
        );

        System.out.println(
                "Historico mudou?..........: "
                        + comparison.historyChanged()
        );

        System.out.println(
                "Boleto mudou?.............: "
                        + comparison.ticketChanged()
        );

        System.out.println(
                "Parcelamento esperado.....: "
                        + result.getInstallmentExpectation()
        );

        System.out.println(
                "POST-BATCH VALIDATION.....: PASS"
        );

        System.out.println(
                "================================================================="
        );
    }

    private static void assertScenarioResult(
            PaymentScenario scenario,
            PaymentPostBatchValidationResult result
    ) {

        switch (scenario) {

            case BELOW_MINIMUM:
                PaymentPostBatchAssertions
                        .assertBelowMinimumPayment(result);
                break;

            case MINIMUM:
                PaymentPostBatchAssertions
                        .assertMinimumPayment(result);
                break;

            case BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT:
            case BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT:
                PaymentPostBatchAssertions
                        .assertPartialPayment(result);
                break;

            case TOTAL:
                PaymentPostBatchAssertions
                        .assertFullPayment(result);
                break;

            case ABOVE_TOTAL:
                throw new IllegalArgumentException(
                        "ABOVE_TOTAL post-batch behavior is not confirmed."
                );

            default:
                throw new IllegalArgumentException(
                        "Unsupported payment scenario: "
                                + scenario
                );
        }
    }

    private static PaymentScenario resolveScenario(
            String value
    ) {

        String scenario =
                value.trim()
                        .toUpperCase();

        switch (scenario) {

            case "BELOW_MINIMUM":
                return PaymentScenario.BELOW_MINIMUM;

            case "MINIMUM":
                return PaymentScenario.MINIMUM;

            case "PARTIAL_20":
            case "BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT":
                return PaymentScenario
                        .BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT;

            case "PARTIAL_60":
            case "BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT":
                return PaymentScenario
                        .BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT;

            case "TOTAL":
                return PaymentScenario.TOTAL;

            case "ABOVE_TOTAL":
                return PaymentScenario.ABOVE_TOTAL;

            default:
                throw new IllegalArgumentException(
                        "Unsupported financial.scenario: "
                                + value
                );
        }
    }

    private static void validatePaymentLink(
            PaymentEventDetails payment,
            AccountId expectedAccountId,
            BigDecimal expectedAmount,
            String expectedNossoNumero
    ) {

        if (!payment.getAccountId()
                .equals(expectedAccountId)) {

            throw new AssertionError(
                    "Payment account differs. Actual="
                            + payment.getAccountId().getValue()
                            + ", expected="
                            + expectedAccountId.getValue()
            );
        }

        if (payment.getAmount()
                .compareTo(expectedAmount) != 0) {

            throw new AssertionError(
                    "Payment amount differs. Actual="
                            + payment.getAmount()
                            + ", expected="
                            + expectedAmount
            );
        }

        if (!payment.getNossoNumero()
                .equals(expectedNossoNumero)) {

            throw new AssertionError(
                    "Payment NossoNumero differs. Actual="
                            + payment.getNossoNumero()
                            + ", expected PRE-BATCH="
                            + expectedNossoNumero
            );
        }
    }

    private static String requireProperty(
            String name
    ) {

        String value =
                System.getProperty(
                        name
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + name
                            + "=..."
            );
        }

        return value.trim();
    }
}