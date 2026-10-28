package com.automation.application.financial.runner;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.payment.execution.PaymentExecutionCoordinator;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.processing.PaymentProcessingStateService;
import com.automation.application.payment.processing.PaymentPostBatchReadinessGuard;
import com.automation.application.payment.scenario.FinancialScenarioPaymentMapper;
import com.automation.application.payment.validation.PaymentPostBatchValidationResult;
import com.automation.application.payment.validation.PaymentPostBatchValidator;
import com.automation.application.payment.validation.PaymentValidationPlan;
import com.automation.application.payment.validation.PaymentValidationPlanFactory;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.FinancialScenario;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.calculator.PaymentAmountCalculator;
import com.automation.domain.payment.model.InvoiceSnapshot;
import com.automation.domain.payment.model.PaymentProcessingState;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

public final class PaymentScenarioExecution {

    private final AccountId accountId;
    private final FinancialScenario scenario;

    private final PaymentAmountCalculator paymentAmountCalculator =
            new PaymentAmountCalculator();

    private final FinancialScenarioPaymentMapper scenarioPaymentMapper =
            new FinancialScenarioPaymentMapper();

    private final PaymentPostBatchReadinessGuard postBatchReadinessGuard =
            new PaymentPostBatchReadinessGuard();
    private FinancialSnapshot beforeSnapshot;
    private FinancialSnapshot afterSnapshot;

    private PaymentInstruction paymentInstruction;
    private PaymentExecutionResult paymentExecutionResult;
    private PaymentProcessingState paymentProcessingState;
    private PaymentValidationPlan validationPlan;
    private PaymentPostBatchValidationResult validationResult;

    PaymentScenarioExecution(
            AccountId accountId,
            FinancialScenario scenario
    ) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "Account ID cannot be null."
        );

        this.scenario = Objects.requireNonNull(
                scenario,
                "Financial scenario cannot be null."
        );
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public FinancialScenario getScenario() {
        return scenario;
    }

    public FinancialSnapshot getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public FinancialSnapshot getAfterSnapshot() {
        return afterSnapshot;
    }

    public PaymentInstruction getPaymentInstruction() {
        return paymentInstruction;
    }

    public PaymentExecutionResult getPaymentExecutionResult() {
        return paymentExecutionResult;
    }

    public PaymentProcessingState getPaymentProcessingState() {
        return paymentProcessingState;
    }

    public PaymentValidationPlan getValidationPlan() {
        return validationPlan;
    }

    public PaymentPostBatchValidationResult getValidationResult() {
        return validationResult;
    }

    public BigDecimal calculateExpectedPaymentAmount() {
        requireBeforeSnapshot();

        PaymentScenario paymentScenario =
                scenarioPaymentMapper.map(
                        scenario
                );

        InvoiceSnapshot invoiceSnapshot =
                new InvoiceSnapshot(
                        accountId.getValue(),
                        beforeSnapshot.getTicketId(),
                        beforeSnapshot.getInvoiceAmount(),
                        beforeSnapshot.getMinimumAmount()
                );

        return paymentAmountCalculator.calculate(
                paymentScenario,
                invoiceSnapshot
        );
    }

    public PaymentScenarioExecution captureBefore(
            FinancialSnapshotService snapshotService
    ) {
        Objects.requireNonNull(
                snapshotService,
                "Financial snapshot service cannot be null."
        );

        beforeSnapshot =
                snapshotService.capture(accountId);

        if (paymentInstruction != null) {
            validatePaymentAmount();
        }

        return this;
    }

    public PaymentScenarioExecution preparePayment(
            FinancialScenarioPaymentMapper scenarioMapper,
            Clock clock
    ) {
        Objects.requireNonNull(
                scenarioMapper,
                "Financial scenario payment mapper cannot be null."
        );

        Objects.requireNonNull(
                clock,
                "Clock cannot be null."
        );

        requireBeforeSnapshot();

        BigDecimal expectedAmount =
                calculateExpectedPaymentAmount();

        paymentInstruction =
                new PaymentInstruction(
                        accountId,
                        scenarioMapper.map(scenario),
                        expectedAmount,
                        clock.instant()
                );

        return this;
    }

    public PaymentScenarioExecution withPaymentInstruction(
            PaymentInstruction paymentInstruction
    ) {
        this.paymentInstruction =
                Objects.requireNonNull(
                        paymentInstruction,
                        "Payment instruction cannot be null."
                );

        validatePaymentAccount();

        validatePaymentAmount();

        return this;
    }

    public PaymentScenarioExecution createValidationPlan(
            PaymentValidationPlanFactory planFactory
    ) {
        Objects.requireNonNull(
                planFactory,
                "Payment validation plan factory cannot be null."
        );

        requireBeforeSnapshot();
        requirePaymentInstruction();

        validationPlan =
                planFactory.create(
                        beforeSnapshot,
                        paymentInstruction
                );

        return this;
    }

    public PaymentScenarioExecution executePayment(
            PaymentExecutionCoordinator executionCoordinator
    ) {
        Objects.requireNonNull(
                executionCoordinator,
                "Payment execution coordinator cannot be null."
        );

        requirePaymentInstruction();

        paymentExecutionResult =
                executionCoordinator.execute(
                        paymentInstruction
                );

        if (paymentExecutionResult == null) {
            throw new IllegalStateException(
                    "Payment execution coordinator returned a null result."
            );
        }

        return this;
    }

    public PaymentScenarioExecution loadPaymentProcessingState(
            PaymentProcessingStateService processingStateService
    ) {
        Objects.requireNonNull(
                processingStateService,
                "Payment processing state service cannot be null."
        );

        requirePaymentExecutionResult();

        paymentProcessingState =
                processingStateService.load(
                        paymentExecutionResult
                );

        return this;
    }
    public PaymentScenarioExecution captureAfter(
            FinancialSnapshotService snapshotService
    ) {
        Objects.requireNonNull(
                snapshotService,
                "Financial snapshot service cannot be null."
        );
        requirePaymentProcessingState();

        postBatchReadinessGuard.requireReady(
                paymentProcessingState
        );

        afterSnapshot =
                snapshotService.capture(accountId);

        return this;
    }

    public PaymentScenarioExecution validatePostBatch(
            PaymentPostBatchValidator validator
    ) {
        Objects.requireNonNull(
                validator,
                "Payment post-batch validator cannot be null."
        );

        requireValidationPlan();
        requireAfterSnapshot();

        validationResult =
                validator.validate(
                        validationPlan,
                        afterSnapshot
                );

        return this;
    }

    private void validatePaymentAccount() {
        if (!accountId.equals(
                paymentInstruction.getAccountId()
        )) {
            throw new IllegalArgumentException(
                    "Payment instruction must belong to scenario account."
            );
        }
    }

    private void validatePaymentAmount() {
        if (beforeSnapshot == null) {
            return;
        }

        BigDecimal expectedAmount =
                calculateExpectedPaymentAmount();

        if (paymentInstruction.getAmount()
                .compareTo(expectedAmount) != 0) {

            throw new IllegalArgumentException(
                    "Payment instruction amount "
                            + paymentInstruction.getAmount()
                            + " does not match scenario "
                            + scenario.getName()
                            + " expected amount "
                            + expectedAmount
                            + "."
            );
        }
    }

    private void requireBeforeSnapshot() {
        if (beforeSnapshot == null) {
            throw new IllegalStateException(
                    "Before snapshot must be captured first."
            );
        }
    }

    private void requirePaymentInstruction() {
        if (paymentInstruction == null) {
            throw new IllegalStateException(
                    "Payment instruction must be provided first."
            );
        }
    }

    private void requireValidationPlan() {
        if (validationPlan == null) {
            throw new IllegalStateException(
                    "Payment validation plan must be created first."
            );
        }
    }

    private void requirePaymentExecutionResult() {
        if (paymentExecutionResult == null) {
            throw new IllegalStateException(
                    "Payment must be executed before loading processing state."
            );
        }
    }
    private void requirePaymentProcessingState() {
        if (paymentProcessingState == null) {
            throw new IllegalStateException(
                    "Payment processing state must be loaded before capturing after snapshot."
            );
        }
    }
    private void requireAfterSnapshot() {
        if (afterSnapshot == null) {
            throw new IllegalStateException(
                    "After snapshot must be captured first."
            );
        }
    }
}

