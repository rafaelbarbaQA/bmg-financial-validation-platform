package com.automation.application.payment.repository;

import com.automation.domain.payment.model.PaymentEventDetails;

import java.util.Optional;

public interface PaymentEventDetailsRepository {

    Optional<PaymentEventDetails> findByPaymentEventId(
            long paymentEventId
    );
}