package com.automation.integration.payment;

import com.automation.domain.account.AccountId;
import com.automation.domain.payment.model.PaymentEventDetails;
import com.automation.infrastructure.database.repository.SqlServerPaymentEventDetailsRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

public final class PaymentEventLinkDatabaseIT {

    @Test
    public void shouldValidateRealPaymentEventLink() {

        long paymentEventId =
                Long.parseLong(
                        requireProperty(
                                "payment.event.id"
                        )
                );

        AccountId expectedAccountId =
                new AccountId(
                        requireProperty(
                                "payment.account.id"
                        )
                );

        BigDecimal expectedAmount =
                new BigDecimal(
                        requireProperty(
                                "payment.amount"
                        )
                );

        String expectedNossoNumero =
                requireProperty(
                        "payment.nosso.numero"
                );

        int expectedStatus =
                Integer.parseInt(
                        System.getProperty(
                                "expected.payment.status",
                                "2"
                        )
                );

        SqlServerPaymentEventDetailsRepository repository =
                new SqlServerPaymentEventDetailsRepository();

        PaymentEventDetails payment =
                repository.findByPaymentEventId(
                                paymentEventId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment event not found: "
                                                + paymentEventId
                                )
                        );

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " PAYMENT EVENT LINK - REAL DATABASE"
        );
        System.out.println(
                "============================================================"
        );

        System.out.println(
                "IdEventoPagamento.......: "
                        + payment.getPaymentEventId()
        );

        System.out.println(
                "IdConta.................: "
                        + payment.getAccountId().getValue()
        );

        System.out.println(
                "ValorPago...............: "
                        + payment.getAmount()
        );

        System.out.println(
                "NossoNumero.............: "
                        + payment.getNossoNumero()
        );

        System.out.println(
                "Status..................: "
                        + payment.getStatus()
        );

        System.out.println(
                "============================================================"
        );

        assertEquals(
                payment.getPaymentEventId(),
                paymentEventId,
                "Unexpected payment event ID."
        );

        assertEquals(
                payment.getAccountId(),
                expectedAccountId,
                "Payment event belongs to a different account."
        );

        assertEquals(
                payment.getAmount().compareTo(
                        expectedAmount
                ),
                0,
                "Payment amount does not match."
        );

        assertEquals(
                payment.getNossoNumero(),
                expectedNossoNumero,
                "Payment NossoNumero does not match."
        );

        assertEquals(
                payment.getStatus(),
                expectedStatus,
                "Payment status does not match."
        );

        System.out.println(
                "PAYMENT EVENT LINK.......: PASS"
        );
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