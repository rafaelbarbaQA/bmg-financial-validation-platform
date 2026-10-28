package com.automation.features.pagamento.model;

public enum TipoBoleto {

    FATURA_MENSAL(7),
    QUITACAO(9);

    private final int codigo;

    TipoBoleto(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    public static TipoBoleto obterPorCodigo(
            int codigo
    ) {

        for (TipoBoleto tipo : values()) {

            if (tipo.codigo == codigo) {
                return tipo;
            }
        }

        throw new IllegalArgumentException(
                "Tipo de boleto não permitido para pagamento: "
                        + codigo
                        + ". São permitidos somente os tipos 7 e 9."
        );
    }
}