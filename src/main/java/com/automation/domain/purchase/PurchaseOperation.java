package com.automation.domain.purchase;

public enum PurchaseOperation {

    CASH(270),
    INSTALLMENT(10557),
    REFINANCING(10376),
    INSTALLMENT_CASH_WITHDRAWAL(10436);

    private final int operationId;

    PurchaseOperation(int operationId) {
        this.operationId = operationId;
    }

    public int getOperationId() {
        return operationId;
    }

    public static PurchaseOperation fromOperationId(
            int operationId
    ) {
        for (PurchaseOperation operation : values()) {

            if (operation.operationId == operationId) {
                return operation;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported purchase operation ID: "
                        + operationId
                        + "."
        );
    }
}