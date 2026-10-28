package com.automation.integration.payment;

import com.automation.application.financial.snapshot.FinancialSnapshotService;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.snapshot.FinancialSnapshot;
import com.automation.domain.payment.PaymentScenario;
import com.automation.domain.payment.calculator.PaymentAmountCalculator;
import com.automation.domain.payment.model.InvoiceSnapshot;
import com.automation.infrastructure.database.repository.SqlServerFinancialSnapshotRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public final class PaymentScenarioCalculationIT {

    @Test
    public void shouldCalculatePaymentScenarioUsingRealInvoiceData() {

        AccountId accountId =
                new AccountId(
                        requireProperty(
                                "payment.account.id"
                        )
                );

        String requestedScenario =
                System.getProperty(
                        "payment.scenario",
                        "ALL"
                )
                .trim()
                .toUpperCase();

        FinancialSnapshotService snapshotService =
                new FinancialSnapshotService(
                        new SqlServerFinancialSnapshotRepository()
                );

        FinancialSnapshot financialSnapshot =
                snapshotService.capture(
                        accountId
                );

        InvoiceSnapshot invoice =
                new InvoiceSnapshot(
                        financialSnapshot
                                .getAccountId()
                                .getValue(),
                        financialSnapshot
                                .getTicketId(),
                        financialSnapshot
                                .getInvoiceAmount(),
                        financialSnapshot
                                .getMinimumAmount()
                );

        PaymentAmountCalculator calculator =
                new PaymentAmountCalculator();

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " PAYMENT SCENARIO CALCULATION - REAL DATABASE"
        );
        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Conta....................: "
                        + financialSnapshot
                                .getAccountId()
                                .getValue()
        );

        System.out.println(
                "Boleto...................: "
                        + financialSnapshot.getTicketId()
        );

        System.out.println(
                "NossoNumero..............: "
                        + financialSnapshot.getNossoNumero()
        );

        System.out.println(
                "Saldo fatura.............: "
                        + invoice.getTotalAmount()
        );

        System.out.println(
                "Valor minimo.............: "
                        + invoice.getMinimumAmount()
        );

        System.out.println(
                "Saldo atual..............: "
                        + financialSnapshot.getCurrentBalance()
        );

        System.out.println(
                "------------------------------------------------------------"
        );

        if ("ALL".equals(requestedScenario)) {

            for (PaymentScenario scenario :
                    PaymentScenario.values()) {

                calculateAndPrint(
                        calculator,
                        scenario,
                        invoice
                );
            }

        } else {

            PaymentScenario scenario =
                    PaymentScenario.valueOf(
                            requestedScenario
                    );

            calculateAndPrint(
                    calculator,
                    scenario,
                    invoice
            );
        }

        System.out.println(
                "============================================================"
        );

        assertNotNull(
                financialSnapshot
        );

        assertNotNull(
                invoice
        );
    }

    private static void calculateAndPrint(
            PaymentAmountCalculator calculator,
            PaymentScenario scenario,
            InvoiceSnapshot invoice
    ) {

        BigDecimal calculatedAmount =
                calculator.calculate(
                        scenario,
                        invoice
                );

        assertNotNull(
                calculatedAmount,
                "Calculated payment amount cannot be null."
        );

        assertTrue(
                calculatedAmount.compareTo(
                        BigDecimal.ZERO
                ) > 0,
                "Calculated payment amount must be greater than zero."
        );

        validateRange(
                scenario,
                calculatedAmount,
                invoice
        );

        System.out.println(
                String.format(
                        "%-38s : %s",
                        scenario.name(),
                        calculatedAmount.toPlainString()
                )
        );
    }

    private static void validateRange(
            PaymentScenario scenario,
            BigDecimal calculatedAmount,
            InvoiceSnapshot invoice
    ) {

        BigDecimal minimum =
                invoice.getMinimumAmount();

        BigDecimal total =
                invoice.getTotalAmount();

        switch (scenario) {

            case TOTAL:

                assertTrue(
                        calculatedAmount.compareTo(total) == 0,
                        "TOTAL must equal invoice total."
                );

                break;

            case MINIMUM:

                assertTrue(
                        calculatedAmount.compareTo(minimum) == 0,
                        "MINIMUM must equal invoice minimum."
                );

                break;

            case BELOW_MINIMUM:

                assertTrue(
                        calculatedAmount.compareTo(minimum) < 0,
                        "BELOW_MINIMUM must be lower than minimum."
                );

                break;

            case BETWEEN_MINIMUM_AND_TOTAL_20_PERCENT:
            case BETWEEN_MINIMUM_AND_TOTAL_60_PERCENT:

                if (total.compareTo(minimum) > 0) {

                    assertTrue(
                            calculatedAmount.compareTo(minimum) > 0,
                            "Partial payment must be greater than minimum."
                    );

                    assertTrue(
                            calculatedAmount.compareTo(total) < 0,
                            "Partial payment must be lower than total."
                    );
                }

                break;

            case ABOVE_TOTAL:

                assertTrue(
                        calculatedAmount.compareTo(total) > 0,
                        "ABOVE_TOTAL must be greater than invoice total."
                );

                break;

            default:

                throw new IllegalArgumentException(
                        "Unsupported payment scenario: "
                                + scenario
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