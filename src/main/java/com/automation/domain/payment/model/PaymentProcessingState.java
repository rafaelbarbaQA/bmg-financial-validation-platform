package com.automation.domain.payment.model;

public final class PaymentProcessingState {

    public static final int WAITING_FOR_BATCH_STATUS = 0;

    public static final int PROCESSED_STATUS = 2;

    private final int status;

    public PaymentProcessingState(
            int status
    ) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public boolean isWaitingForBatch() {
        return status == WAITING_FOR_BATCH_STATUS;
    }

    public boolean isProcessed() {
        return status == PROCESSED_STATUS;
    }
}