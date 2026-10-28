package com.automation.integration.payment;

import io.qameta.allure.Story;
import io.qameta.allure.Feature;
import io.qameta.allure.Epic;
import com.automation.domain.payment.model.PaymentEventDetails;
import com.automation.infrastructure.database.repository.SqlServerPaymentEventDetailsRepository;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;
@Epic("Financial Validation Platform")
@Feature("Pagamentos")

public final class PaymentEventDetailsDatabaseIT {

    @Test
@Story("Persistencia SQL do pagamento")
    public void shouldValidateExistingPaymentEventInDatabase() {

        long paymentEventId =
                requireLongProperty(
                        "payment.event.id"
                );

        String expectedAccount =
                requireProperty(
                        "payment.expected.account"
                );

        String expectedNossoNumero =
                requireProperty(
                        "payment.expected.nossoNumero"
                );

        BigDecimal expectedAmount =
                new BigDecimal(
                        requireProperty(
                                "payment.expected.amount"
                        )
                );

        int expectedStatus =
                Integer.parseInt(
                        System.getProperty(
                                "payment.expected.status",
                                "0"
                        )
                );

        PaymentEventDetails details =
                new SqlServerPaymentEventDetailsRepository()
                        .findByPaymentEventId(
                                paymentEventId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment event "
                                                + paymentEventId
                                                + " was not found."
                                )
                        );

        assertEquals(
                details.getPaymentEventId(),
                paymentEventId,
                "Id_EventoPagamento differs."
        );

        assertEquals(
                details.getAccountId().getValue(),
                expectedAccount,
                "Id_Conta differs."
        );

        assertEquals(
                details.getNossoNumero(),
                expectedNossoNumero,
                "NossoNumero differs."
        );

        assertEquals(
                details.getAmount().compareTo(
                        expectedAmount
                ),
                0,
                "ValorPago differs."
        );

        assertEquals(
                details.getStatus(),
                expectedStatus,
                "Payment status differs."
        );

        System.out.println();
        System.out.println(
                "================ PAYMENT DATABASE VALIDATION ================"
        );

        System.out.println(
                "IdEventoPagamento.........: "
                        + details.getPaymentEventId()
        );

        System.out.println(
                "Conta.....................: "
                        + details.getAccountId().getValue()
        );

        System.out.println(
                "NossoNumero...............: "
                        + details.getNossoNumero()
        );

        System.out.println(
                "ValorPago..................: "
                        + details.getAmount()
        );

        System.out.println(
                "Status.....................: "
                        + details.getStatus()
        );

        System.out.println(
                "DATABASE VALIDATION.......: PASS"
        );

        System.out.println(
                "============================================================="
        );
    }

    private static String requireProperty(
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

        return value.trim();
    }

    private static long requireLongProperty(
            String propertyName
    ) {

        String value =
                requireProperty(
                        propertyName
                );

        try {

            long parsed =
                    Long.parseLong(
                            value
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