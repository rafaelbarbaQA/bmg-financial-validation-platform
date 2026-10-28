package com.automation.infrastructure.database.repository;

import com.automation.application.payment.repository.PaymentEventDetailsRepository;
import com.automation.core.database.DatabaseAccessor;
import com.automation.domain.account.AccountId;
import com.automation.domain.payment.model.PaymentEventDetails;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SqlServerPaymentEventDetailsRepository
        implements PaymentEventDetailsRepository {

    private static final String FIND_BY_PAYMENT_EVENT_ID =
            "SELECT "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO, "
                    + "Id_Conta AS ID_CONTA, "
                    + "ValorPago AS VALOR_PAGO, "
                    + "NossoNumero AS NOSSO_NUMERO, "
                    + "Status AS STATUS "
                    + "FROM EventosExternosPagamentos "
                    + "WHERE Id_EventoPagamento = ?";

    private final DatabaseAccessor databaseAccessor;

    public SqlServerPaymentEventDetailsRepository() {
        this(
                new DatabaseAccessor()
        );
    }

    public SqlServerPaymentEventDetailsRepository(
            DatabaseAccessor databaseAccessor
    ) {

        this.databaseAccessor = Objects.requireNonNull(
                databaseAccessor,
                "DatabaseAccessor cannot be null."
        );
    }

    @Override
    public Optional<PaymentEventDetails> findByPaymentEventId(
            long paymentEventId
    ) {

        if (paymentEventId <= 0) {
            throw new IllegalArgumentException(
                    "Payment event ID must be greater than zero."
            );
        }

        try {

            Optional<Map<String, Object>> result =
                    databaseAccessor.executeQuery(
                            FIND_BY_PAYMENT_EVENT_ID,
                            paymentEventId
                    );

            if (result.isEmpty()) {
                return Optional.empty();
            }

            Map<String, Object> row =
                    result.get();

            return Optional.of(
                    new PaymentEventDetails(
                            requireNumber(
                                    row,
                                    "ID_EVENTO_PAGAMENTO"
                            ).longValue(),
                            new AccountId(
                                    requireText(
                                            row,
                                            "ID_CONTA"
                                    )
                            ),
                            requireBigDecimal(
                                    row,
                                    "VALOR_PAGO"
                            ),
                            requireText(
                                    row,
                                    "NOSSO_NUMERO"
                            ),
                            requireNumber(
                                    row,
                                    "STATUS"
                            ).intValue()
                    )
            );

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not query payment event "
                            + paymentEventId
                            + ".",
                    exception
            );
        }
    }

    private static Number requireNumber(
            Map<String, Object> row,
            String column
    ) {

        Object value = row.get(column);

        if (!(value instanceof Number)) {
            throw new IllegalStateException(
                    column
                            + " was not returned as a numeric value. Actual: "
                            + value
            );
        }

        return (Number) value;
    }

    private static BigDecimal requireBigDecimal(
            Map<String, Object> row,
            String column
    ) {

        Object value = row.get(column);

        if (value == null) {
            throw new IllegalStateException(
                    column + " was returned as null."
            );
        }

        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }

        if (value instanceof Number) {
            return new BigDecimal(
                    value.toString()
            );
        }

        throw new IllegalStateException(
                column
                        + " was not returned as a numeric value. Actual: "
                        + value
        );
    }

    private static String requireText(
            Map<String, Object> row,
            String column
    ) {

        Object value = row.get(column);

        if (value == null) {
            throw new IllegalStateException(
                    column + " was returned as null."
            );
        }

        String text =
                value.toString().trim();

        if (text.isEmpty()) {
            throw new IllegalStateException(
                    column + " was returned as blank."
            );
        }

        return text;
    }
}