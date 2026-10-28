package com.automation.infrastructure.database.repository;

import com.automation.application.payment.repository.PaymentProcessingStateRepository;
import com.automation.core.database.DatabaseAccessor;
import com.automation.domain.payment.model.PaymentProcessingState;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SqlServerPaymentProcessingStateRepository
        implements PaymentProcessingStateRepository {

    private static final String FIND_BY_PAYMENT_EVENT_ID =
            "SELECT "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO, "
                    + "Status AS STATUS "
                    + "FROM EventosExternosPagamentos "
                    + "WHERE Id_EventoPagamento = ?";

    private final DatabaseAccessor databaseAccessor;

    public SqlServerPaymentProcessingStateRepository() {
        this(
                new DatabaseAccessor()
        );
    }

    public SqlServerPaymentProcessingStateRepository(
            DatabaseAccessor databaseAccessor
    ) {
        this.databaseAccessor = Objects.requireNonNull(
                databaseAccessor,
                "DatabaseAccessor cannot be null."
        );
    }

    @Override
    public Optional<PaymentProcessingState> findByPaymentEventId(
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

            Object statusValue =
                    result.get().get("STATUS");

            if (!(statusValue instanceof Number)) {
                throw new IllegalStateException(
                        "Payment STATUS was not returned as a numeric value."
                );
            }

            return Optional.of(
                    new PaymentProcessingState(
                            ((Number) statusValue).intValue()
                    )
            );

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not query payment processing state for event "
                            + paymentEventId
                            + ".",
                    exception
            );
        }
    }
}
