package com.automation.application.financial.runner;

import com.automation.application.financial.snapshot.FinancialSnapshotRepository;
import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.payment.execution.PaymentExecutionCoordinator;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.execution.PaymentExecutor;
import com.automation.application.payment.processing.PaymentProcessingStateService;
import com.automation.application.payment.repository.PaymentProcessingStateRepository;
import com.automation.application.payment.scenario.FinancialScenarioPaymentMapper;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.domain.payment.model.PaymentProcessingState;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.expectThrows;

public final class PaymentScenarioExecutionTest {

    private static final AccountId ACCOUNT_ID =
            new AccountId("13714323");

    private static final Instant FIXED_TIME =
            Instant.parse("2026-08-07T12:00:00Z");

    private FinancialSnapshotService snapshotService;

    private FinancialScenarioPaymentMapper mapper;

    private Clock clock;

    @BeforeMethod
    public void setUp() {

        FinancialSnapshot snapshot =
                createSnapshot();

        FinancialSnapshotRepository repository =
                accountId -> snapshot;

        snapshotService =
                new FinancialSnapshotService(
                        repository
                );

        mapper =
                new FinancialScenarioPaymentMapper();

        clock =
                Clock.fixed(
                        FIXED_TIME,
                        ZoneOffset.UTC
                );
    }

    @Test
    public void shouldPreparePaymentBelowMinimum() {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .belowMinimumPayment(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        assertPayment(
                execution.getPaymentInstruction(),
                PaymentScenario.BELOW_MINIMUM,
                "100.00"
        );
    }

    @Test
    public void shouldPrepareMinimumPayment() {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .minimumPayment(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        assertPayment(
                execution.getPaymentInstruction(),
                PaymentScenario.MINIMUM,
                "200.00"
        );
    }

    @Test
    public void shouldPreparePartialTwentyPercentPayment() {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .partialPayment20(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        assertPayment(
                execution.getPaymentInstruction(),
                PaymentScenario
                        .BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT,
                "360.00"
        );
    }

    @Test
    public void shouldPreparePartialSixtyPercentPayment() {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .partialPayment60(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        assertPayment(
                execution.getPaymentInstruction(),
                PaymentScenario
                        .BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT,
                "680.00"
        );
    }

    @Test
    public void shouldUseInvoiceAmountInsteadOfCurrentAccountBalanceForFullPayment() {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .fullPayment(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        assertPayment(
                execution.getPaymentInstruction(),
                PaymentScenario.TOTAL,
                "1000.00"
        );
    }

    @Test
    public void shouldRejectAfterSnapshotWhenProcessingStateWasNotLoaded() {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .fullPayment(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        IllegalStateException exception =
                expectThrows(
                        IllegalStateException.class,
                        () -> execution.captureAfter(
                                snapshotService
                        )
                );

        assertEquals(
                exception.getMessage(),
                "Payment processing state must be loaded before capturing after snapshot."
        );
    }

    @Test
    public void shouldRejectAfterSnapshotWhilePaymentIsWaitingForBatch() {

        PaymentScenarioExecution execution =
                createExecutionWithProcessingStatus(
                        0
                );

        IllegalStateException exception =
                expectThrows(
                        IllegalStateException.class,
                        () -> execution.captureAfter(
                                snapshotService
                        )
                );

        assertEquals(
                exception.getMessage(),
                "Payment is waiting for batch processing. Current status: 0."
        );
    }

    @Test
    public void shouldCaptureAfterSnapshotWhenPaymentIsProcessed() {

        PaymentScenarioExecution execution =
                createExecutionWithProcessingStatus(
                        2
                );

        execution.captureAfter(
                snapshotService
        );

        assertNotNull(
                execution.getAfterSnapshot()
        );

        assertEquals(
                execution.getAfterSnapshot()
                        .getCurrentBalance(),
                new BigDecimal("1200.00")
        );
    }

    @Test
    public void shouldRejectAfterSnapshotForUnexpectedProcessingStatus() {

        PaymentScenarioExecution execution =
                createExecutionWithProcessingStatus(
                        99
                );

        IllegalStateException exception =
                expectThrows(
                        IllegalStateException.class,
                        () -> execution.captureAfter(
                                snapshotService
                        )
                );

        assertEquals(
                exception.getMessage(),
                "Payment has an unexpected processing status: 99."
        );
    }

    private PaymentScenarioExecution createExecutionWithProcessingStatus(
            int status
    ) {

        PaymentScenarioExecution execution =
                FinancialScenarioRunner
                        .fullPayment(ACCOUNT_ID)
                        .captureBefore(snapshotService)
                        .preparePayment(mapper, clock);

        PaymentExecutor executor =
                instruction ->
                        PaymentExecutionResult.success(
                                instruction,
                                "123",
                                "Fake successful payment execution.",
                                FIXED_TIME
                        );

        PaymentExecutionCoordinator coordinator =
                new PaymentExecutionCoordinator(
                        executor
                );

        PaymentProcessingStateRepository processingRepository =
                paymentEventId ->
                        Optional.of(
                                new PaymentProcessingState(
                                        status
                                )
                        );

        PaymentProcessingStateService processingService =
                new PaymentProcessingStateService(
                        processingRepository
                );

        return execution
                .executePayment(
                        coordinator
                )
                .loadPaymentProcessingState(
                        processingService
                );
    }

    private void assertPayment(
            PaymentInstruction instruction,
            PaymentScenario expectedScenario,
            String expectedAmount
    ) {

        assertEquals(
                instruction.getAccountId(),
                ACCOUNT_ID
        );

        assertEquals(
                instruction.getScenario(),
                expectedScenario
        );

        assertEquals(
                instruction.getAmount(),
                new BigDecimal(expectedAmount)
        );

        assertEquals(
                instruction.getPreparedAt(),
                FIXED_TIME
        );
    }

    private FinancialSnapshot createSnapshot() {

        return new FinancialSnapshot(
                ACCOUNT_ID,
                0,
                "877716729",
                "568338116",
                "9268419047",
                new BigDecimal("1200.00"),
                new BigDecimal("200.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 10),
                FIXED_TIME
        );
    }
}