package com.automation.features.pagamento.model;

public final class PagamentoExistente {

    private final long idEventoPagamento;
    private final int status;

    public PagamentoExistente(
            long idEventoPagamento,
            int status
    ) {

        if (idEventoPagamento <= 0) {
            throw new IllegalArgumentException(
                    "O IdEventoPagamento deve ser "
                            + "maior que zero."
            );
        }

        if (status < 0) {
            throw new IllegalArgumentException(
                    "O status do pagamento não pode "
                            + "ser negativo."
            );
        }

        this.idEventoPagamento =
                idEventoPagamento;

        this.status =
                status;
    }

    public long getIdEventoPagamento() {
        return idEventoPagamento;
    }

    public int getStatus() {
        return status;
    }

    public String getDescricaoStatus() {

        return StatusEventoPagamento.descrever(
                status
        );
    }

    public boolean deveBloquearDuplicidade() {

        return StatusEventoPagamento
                .deveBloquearDuplicidade(
                        status
                );
    }

    @Override
    public String toString() {

        return "PagamentoExistente{"
                + "idEventoPagamento="
                + idEventoPagamento
                + ", status="
                + status
                + ", descricaoStatus='"
                + getDescricaoStatus()
                + '\''
                + ", bloqueiaDuplicidade="
                + deveBloquearDuplicidade()
                + '}';
    }
}