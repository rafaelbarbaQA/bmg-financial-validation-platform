package com.automation.features.pagamento.model;

import java.math.BigDecimal;

public final class BoletoParaPagamento {

    private static final int TAMANHO_MAXIMO_NOSSO_NUMERO =
            20;

    private final long idBoleto;
    private final TipoBoleto tipoBoleto;
    private final long idConta;
    private final BigDecimal valorBoleto;
    private final String nossoNumero;

    public BoletoParaPagamento(
            long idBoleto,
            TipoBoleto tipoBoleto,
            long idConta,
            BigDecimal valorBoleto,
            String nossoNumero
    ) {

        validar(
                idBoleto,
                tipoBoleto,
                idConta,
                valorBoleto,
                nossoNumero
        );

        this.idBoleto = idBoleto;
        this.tipoBoleto = tipoBoleto;
        this.idConta = idConta;
        this.valorBoleto = valorBoleto;
        this.nossoNumero = nossoNumero.trim();
    }

    private void validar(
            long idBoleto,
            TipoBoleto tipoBoleto,
            long idConta,
            BigDecimal valorBoleto,
            String nossoNumero
    ) {

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "O IdBoleto deve ser maior que zero."
            );
        }

        if (tipoBoleto == null) {
            throw new IllegalArgumentException(
                    "O TipoBoleto não pode ser nulo."
            );
        }

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }

        if (valorBoleto == null
                || valorBoleto.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "O ValorBoleto deve ser maior que zero."
            );
        }

        if (nossoNumero == null
                || nossoNumero.isBlank()) {

            throw new IllegalArgumentException(
                    "O NossoNumero não pode ser vazio."
            );
        }

        String numeroNormalizado =
                nossoNumero.trim();

        if (!numeroNormalizado.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "O NossoNumero deve conter somente números."
            );
        }

        if (numeroNormalizado.length()
                > TAMANHO_MAXIMO_NOSSO_NUMERO) {

            throw new IllegalArgumentException(
                    "O NossoNumero possui "
                            + numeroNormalizado.length()
                            + " dígitos, mas a tabela de pagamentos "
                            + "aceita no máximo "
                            + TAMANHO_MAXIMO_NOSSO_NUMERO
                            + "."
            );
        }
    }

    public long getIdBoleto() {
        return idBoleto;
    }

    public TipoBoleto getTipoBoleto() {
        return tipoBoleto;
    }

    public long getIdConta() {
        return idConta;
    }

    public BigDecimal getValorBoleto() {
        return valorBoleto;
    }

    public String getNossoNumero() {
        return nossoNumero;
    }
}