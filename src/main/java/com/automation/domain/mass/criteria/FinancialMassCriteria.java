package com.automation.domain.mass.criteria;

import java.math.BigDecimal;
import java.util.Objects;

public final class FinancialMassCriteria {

    private final Integer accountStatus;
    private final Integer productId;
    private final InstallmentRequirement installmentRequirement;
    private final BigDecimal minimumAmountGreaterThan;

    /**
     * Mantido para compatibilidade com o código já criado.
     */
    public FinancialMassCriteria(
            int accountStatus,
            InstallmentRequirement installmentRequirement
    ) {
        this(
                Integer.valueOf(accountStatus),
                null,
                installmentRequirement,
                null
        );
    }

    private FinancialMassCriteria(
            Integer accountStatus,
            Integer productId,
            InstallmentRequirement installmentRequirement,
            BigDecimal minimumAmountGreaterThan
    ) {
        this.accountStatus = validateStatus(accountStatus);
        this.productId = validateProductId(productId);

        this.installmentRequirement = Objects.requireNonNull(
                installmentRequirement,
                "Installment requirement cannot be null."
        );

        this.minimumAmountGreaterThan =
                validateMinimumAmount(minimumAmountGreaterThan);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getAccountStatus() {
        return accountStatus;
    }

    public Integer getProductId() {
        return productId;
    }

    public InstallmentRequirement getInstallmentRequirement() {
        return installmentRequirement;
    }

    public BigDecimal getMinimumAmountGreaterThan() {
        return minimumAmountGreaterThan;
    }

    public boolean hasAccountStatus() {
        return accountStatus != null;
    }

    public boolean hasProductId() {
        return productId != null;
    }

    public boolean hasMinimumAmountGreaterThan() {
        return minimumAmountGreaterThan != null;
    }

    private static Integer validateStatus(Integer accountStatus) {
        if (accountStatus != null && accountStatus.intValue() < 0) {
            throw new IllegalArgumentException(
                    "Account status cannot be negative."
            );
        }

        return accountStatus;
    }

    private static Integer validateProductId(Integer productId) {
        if (productId != null && productId.intValue() <= 0) {
            throw new IllegalArgumentException(
                    "Product ID must be greater than zero."
            );
        }

        return productId;
    }

    private static BigDecimal validateMinimumAmount(
            BigDecimal minimumAmount
    ) {
        if (minimumAmount != null
                && minimumAmount.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Minimum amount cannot be negative."
            );
        }

        return minimumAmount;
    }

    public static final class Builder {

        private Integer accountStatus;
        private Integer productId;
        private InstallmentRequirement installmentRequirement =
                InstallmentRequirement.ANY;
        private BigDecimal minimumAmountGreaterThan;

        private Builder() {
        }

        public Builder accountStatus(int accountStatus) {
            this.accountStatus = Integer.valueOf(accountStatus);
            return this;
        }

        public Builder productId(int productId) {
            this.productId = Integer.valueOf(productId);
            return this;
        }

        public Builder withInstallments() {
            this.installmentRequirement =
                    InstallmentRequirement.REQUIRED;
            return this;
        }

        public Builder withoutInstallments() {
            this.installmentRequirement =
                    InstallmentRequirement.FORBIDDEN;
            return this;
        }

        public Builder anyInstallmentCondition() {
            this.installmentRequirement =
                    InstallmentRequirement.ANY;
            return this;
        }

        public Builder minimumAmountGreaterThan(
                BigDecimal minimumAmount
        ) {
            this.minimumAmountGreaterThan =
                    Objects.requireNonNull(
                            minimumAmount,
                            "Minimum amount filter cannot be null."
                    );

            return this;
        }

        public FinancialMassCriteria build() {
            return new FinancialMassCriteria(
                    accountStatus,
                    productId,
                    installmentRequirement,
                    minimumAmountGreaterThan
            );
        }
    }
}