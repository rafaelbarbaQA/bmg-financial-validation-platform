package com.automation.features.pagamento.model;

import java.util.Optional;

public enum StatusEventoPagamento {

    PENDENTE(
            0,
            "Pendente",
            true
    ),

    PROCESSADO(
            2,
            "Processado",
            true
    );

    private final int codigo;
    private final String descricao;
    private final boolean bloqueiaDuplicidade;

    StatusEventoPagamento(
            int codigo,
            String descricao,
            boolean bloqueiaDuplicidade
    ) {

        this.codigo =
                codigo;

        this.descricao =
                descricao;

        this.bloqueiaDuplicidade =
                bloqueiaDuplicidade;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean bloqueiaDuplicidade() {
        return bloqueiaDuplicidade;
    }

    public static Optional<StatusEventoPagamento>
    tentarObterPorCodigo(
            int codigo
    ) {

        for (
                StatusEventoPagamento status
                : values()
        ) {

            if (status.getCodigo() == codigo) {
                return Optional.of(
                        status
                );
            }
        }

        return Optional.empty();
    }

    public static String descrever(
            int codigo
    ) {

        Optional<StatusEventoPagamento> status =
                tentarObterPorCodigo(
                        codigo
                );

        if (status.isPresent()) {
            return status.get()
                    .getDescricao();
        }

        return "Desconhecido ("
                + codigo
                + ")";
    }

    public static boolean deveBloquearDuplicidade(
            int codigo
    ) {

        Optional<StatusEventoPagamento> status =
                tentarObterPorCodigo(
                        codigo
                );

        if (status.isPresent()) {
            return status.get()
                    .bloqueiaDuplicidade();
        }

        /*
         * Um status ainda não mapeado bloqueia por segurança.
         * Quando seu significado for conhecido, ele deve ser
         * adicionado ao enum com a política adequada.
         */
        return true;
    }
}