package com.automation.application.payment.repository;

import com.automation.domain.payment.model.PaymentProcessingState;

import java.util.Optional;

public interface PaymentProcessingStateRepository {

    Optional<PaymentProcessingState> findByPaymentEventId(
            long paymentEventId
    );
}
