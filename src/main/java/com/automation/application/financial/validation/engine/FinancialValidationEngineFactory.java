package com.automation.application.financial.validation.engine;

import com.automation.application.financial.validation.balance.BalanceValidationService;
import com.automation.application.financial.validation.balance.FullPaymentBalanceExpectationCalculator;
import com.automation.application.financial.validation.step.AccountStatusValidationStep;
import com.automation.application.financial.validation.step.BalanceValidationStep;
import com.automation.application.financial.validation.step.FinancialValidationStep;
import com.automation.application.financial.validation.step.InstallmentAdhesionValidationStep;
import com.automation.application.financial.validation.step.InstallmentOfferValidationStep;
import com.automation.application.financial.validation.step.PaymentProcessingStatusValidationStep;
import com.automation.application.financial.validation.validator.AccountStatusValidator;
import com.automation.application.financial.validation.validator.BalanceValidator;
import com.automation.application.financial.validation.validator.InstallmentAdhesionValidator;
import com.automation.application.financial.validation.validator.InstallmentOfferValidator;
import com.automation.application.financial.validation.validator.PaymentProcessingStatusValidator;

import java.util.ArrayList;
import java.util.List;

public final class FinancialValidationEngineFactory {

    private FinancialValidationEngineFactory() {
    }

    public static FinancialValidationEngine forFullPayment() {

        return new FinancialValidationEngine(
                fullPaymentSteps()
        );
    }

    public static FinancialValidationEngine forFullPayment(
            int expectedAccountStatus
    ) {

        List<FinancialValidationStep> steps =
                new ArrayList<FinancialValidationStep>(
                        fullPaymentSteps()
                );

        steps.add(
                accountStatusStep(
                        expectedAccountStatus
                )
        );

        return new FinancialValidationEngine(
                steps
        );
    }

    public static FinancialValidationEngine forFullPaymentWithBalance() {

        return new FinancialValidationEngine(
                fullPaymentWithBalanceSteps()
        );
    }

    public static FinancialValidationEngine forFullPaymentWithBalance(
            int expectedAccountStatus
    ) {

        List<FinancialValidationStep> steps =
                new ArrayList<FinancialValidationStep>(
                        fullPaymentWithBalanceSteps()
                );

        steps.add(
                accountStatusStep(
                        expectedAccountStatus
                )
        );

        return new FinancialValidationEngine(
                steps
        );
    }
    /*
     * Checkpoint 1:
     * dia do vencimento.
     */
    public static FinancialValidationEngine forInstallmentOffer(
            boolean expectedOffer
    ) {

        return new FinancialValidationEngine(
                List.of(
                        new InstallmentOfferValidationStep(
                                expectedOffer,
                                new InstallmentOfferValidator()
                        )
                )
        );
    }

    /*
     * Checkpoint 2:
     * quinto batch apos o vencimento.
     */
    public static FinancialValidationEngine forInstallmentAdhesion() {

        return new FinancialValidationEngine(
                installmentAdhesionSteps()
        );
    }

    public static FinancialValidationEngine forInstallmentAdhesion(
            int expectedAccountStatus
    ) {

        List<FinancialValidationStep> steps =
                new ArrayList<FinancialValidationStep>(
                        installmentAdhesionSteps()
                );

        steps.add(
                accountStatusStep(
                        expectedAccountStatus
                )
        );

        return new FinancialValidationEngine(
                steps
        );
    }

    private static List<FinancialValidationStep> fullPaymentSteps() {

        return List.of(
                paymentStatusStep()
        );
    }

    private static List<FinancialValidationStep> fullPaymentWithBalanceSteps() {

        return List.of(
                paymentStatusStep(),
                balanceStep()
        );
    }
    private static List<FinancialValidationStep> installmentAdhesionSteps() {

        return List.of(
                paymentStatusStep(),
                new InstallmentAdhesionValidationStep(
                        new InstallmentAdhesionValidator()
                )
        );
    }

    private static FinancialValidationStep paymentStatusStep() {

        return new PaymentProcessingStatusValidationStep(
                new PaymentProcessingStatusValidator()
        );
    }

    private static FinancialValidationStep balanceStep() {

        return new BalanceValidationStep(
                new BalanceValidationService(
                        new FullPaymentBalanceExpectationCalculator(),
                        new BalanceValidator()
                )
        );
    }
    private static FinancialValidationStep accountStatusStep(
            int expectedAccountStatus
    ) {

        return new AccountStatusValidationStep(
                expectedAccountStatus,
                new AccountStatusValidator()
        );
    }
}