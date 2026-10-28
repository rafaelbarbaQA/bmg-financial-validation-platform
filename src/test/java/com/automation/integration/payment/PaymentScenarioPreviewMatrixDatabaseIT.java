package com.automation.integration.payment;

import com.automation.application.financial.runner.FinancialScenarioRunner;
import com.automation.application.financial.runner.PaymentScenarioExecution;
import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.application.payment.execution.PaymentExecutionCoordinator;
import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.scenario.FinancialScenarioPaymentMapper;
import com.automation.application.payment.validation.PaymentValidationPlan;
import com.automation.application.payment.validation.PaymentValidationPlanFactory;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import com.automation.infrastructure.payment.execution.PreviewPaymentExecutor;
import org.testng.annotations.Test;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class PaymentScenarioPreviewMatrixDatabaseIT {

    @Test
    public void shouldPrepareValidationPlansAndExecuteScenarioMatrixInPreviewMode() {

        AccountId accountId =
                new AccountId(
                        requireProperty(
                                "payment.account.id"
                        )
                );

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialScenarioPaymentMapper mapper =
                new FinancialScenarioPaymentMapper();

        PaymentValidationPlanFactory planFactory =
                new PaymentValidationPlanFactory();

        Clock clock =
                Clock.systemUTC();

        PaymentExecutionCoordinator coordinator =
                new PaymentExecutionCoordinator(
                        new PreviewPaymentExecutor()
                );

        List<PaymentScenarioExecution> executions =
                Arrays.asList(
                        FinancialScenarioRunner
                                .belowMinimumPayment(accountId),

                        FinancialScenarioRunner
                                .minimumPayment(accountId),

                        FinancialScenarioRunner
                                .partialPayment20(accountId),

                        FinancialScenarioRunner
                                .partialPayment60(accountId),

                        FinancialScenarioRunner
                                .fullPayment(accountId)
                );

        System.out.println();
        System.out.println(
                "================================================================================================================"
        );

        System.out.println(
                " FINANCIAL PAYMENT PIPELINE - REAL DATABASE / PREVIEW"
        );

        System.out.println(
                "================================================================================================================"
        );

        boolean snapshotPrinted = false;

        for (PaymentScenarioExecution execution : executions) {

            execution
                    .captureBefore(snapshotService)
                    .preparePayment(
                            mapper,
                            clock
                    )
                    .createValidationPlan(
                            planFactory
                    )
                    .executePayment(
                            coordinator
                    );

            FinancialSnapshot snapshot =
                    execution.getBeforeSnapshot();

            PaymentInstruction instruction =
                    execution.getPaymentInstruction();

            PaymentValidationPlan plan =
                    execution.getValidationPlan();

            PaymentExecutionResult result =
                    execution.getPaymentExecutionResult();

            if (!snapshotPrinted) {

                System.out.println(
                        "Conta....................: "
                                + snapshot
                                .getAccountId()
                                .getValue()
                );

                System.out.println(
                        "Saldo atual..............: "
                                + snapshot
                                .getCurrentBalance()
                );

                System.out.println(
                        "Saldo fatura.............: "
                                + snapshot
                                .getInvoiceAmount()
                );

                System.out.println(
                        "Valor minimo.............: "
                                + snapshot
                                .getMinimumAmount()
                );

                System.out.println(
                        "Boleto...................: "
                                + snapshot
                                .getTicketId()
                );

                System.out.println(
                        "NossoNumero..............: "
                                + snapshot
                                .getNossoNumero()
                );

                System.out.println(
                        "----------------------------------------------------------------------------------------------------------------"
                );

                snapshotPrinted = true;
            }


            assertEquals(
                    instruction.getAccountId(),
                    accountId
            );
assertTrue(
                    instruction.getAmount()
                            .signum() > 0
            );

            assertNotNull(
                    plan
            );

            assertEquals(
                    plan.getScenario(),
                    instruction.getScenario()
            );

            assertEquals(
                    plan.getPaymentAmount()
                            .compareTo(
                                    instruction.getAmount()
                            ),
                    0
            );

            assertEquals(
                    plan.getBeforeSnapshot()
                            .getAccountId(),
                    accountId
            );

            assertNotNull(
                    result
            );

            assertTrue(
                    result.isSuccessful()
            );

            assertTrue(
                    result.getExternalPaymentId()
                            .startsWith(
                                    "PREVIEW-"
                            )
            );
        }

        System.out.println(
                "----------------------------------------------------------------------------------------------------------------"
        );

        System.out.println(
                "BEFORE SNAPSHOT..........: CAPTURED"
        );

        System.out.println(
                "PAYMENT VALUES...........: CALCULATED"
        );

        System.out.println(
                "VALIDATION PLANS.........: CREATED"
        );

        System.out.println(
                "EXECUTION MODE...........: PREVIEW"
        );

        System.out.println(
                "REAL PAYMENT.............: NOT EXECUTED"
        );

        System.out.println(
                "POST-BATCH...............: PENDING REAL PROCESSING"
        );

        System.out.println(
                "================================================================================================================"
        );
    }

    private static String requireProperty(
            String property
    ) {

        String value =
                System.getProperty(
                        property
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + property
                            + "=..."
            );
        }

        return value.trim();
    }
}