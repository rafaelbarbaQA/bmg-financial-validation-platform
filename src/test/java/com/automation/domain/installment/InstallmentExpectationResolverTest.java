package com.automation.domain.installment;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

public class InstallmentExpectationResolverTest {

    private final InstallmentExpectationResolver resolver =
            new InstallmentExpectationResolver();

    @Test
    public void shouldNotCreateInstallmentWhenPaymentIsBelowMinimum() {

        InstallmentExpectation expectation =
                resolver.resolve(
                        new BigDecimal("100"),
                        new BigDecimal("200"),
                        new BigDecimal("1000")
                );

        assertEquals(
                expectation,
                InstallmentExpectation.NO_INSTALLMENT_ADHESION
        );
    }

    @Test
    public void shouldAcceptOriginalOfferWhenPaymentEqualsMinimum() {

        InstallmentExpectation expectation =
                resolver.resolve(
                        new BigDecimal("200"),
                        new BigDecimal("200"),
                        new BigDecimal("1000")
                );

        assertEquals(
                expectation,
                InstallmentExpectation.ORIGINAL_OFFER_ADHESION
        );
    }

    @Test
    public void shouldCreateRecalculatedOfferWhenPaymentIsAboveMinimum() {

        InstallmentExpectation expectation =
                resolver.resolve(
                        new BigDecimal("500"),
                        new BigDecimal("200"),
                        new BigDecimal("1000")
                );

        assertEquals(
                expectation,
                InstallmentExpectation.RECALCULATED_OFFER_ADHESION
        );
    }

}