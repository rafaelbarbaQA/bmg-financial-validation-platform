package com.automation.features.pagamento.testdata;

import com.automation.features.pagamento.model.ModalidadePagamento;

public final class CenarioPagamentoBoleto {

    private final String nome;
    private final long idBoleto;
    private final ModalidadePagamento modalidade;
    private final boolean ativo;

    public CenarioPagamentoBoleto(
            String nome,
            long idBoleto,
            ModalidadePagamento modalidade,
            boolean ativo
    ) {

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do cenário não pode ser vazio."
            );
        }

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "O IdBoleto deve ser maior que zero."
            );
        }

        if (modalidade == null) {
            throw new IllegalArgumentException(
                    "A modalidade de pagamento "
                            + "não pode ser nula."
            );
        }

        this.nome =
                nome.trim();

        this.idBoleto =
                idBoleto;

        this.modalidade =
                modalidade;

        this.ativo =
                ativo;
    }

    public String getNome() {
        return nome;
    }

    public long getIdBoleto() {
        return idBoleto;
    }

    public ModalidadePagamento getModalidade() {
        return modalidade;
    }

    public boolean isAtivo() {
        return ativo;
    }

    @Override
    public String toString() {

        return "CenarioPagamentoBoleto{"
                + "nome='"
                + nome
                + '\''
                + ", idBoleto="
                + idBoleto
                + ", modalidade="
                + modalidade
                + ", ativo="
                + ativo
                + '}';
    }
}