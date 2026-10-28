package com.automation.application.payment.processing;

public enum PaymentProcessingReadiness {

    WAITING_FOR_BATCH,

    READY_FOR_POST_BATCH,

    UNEXPECTED_STATUS
}