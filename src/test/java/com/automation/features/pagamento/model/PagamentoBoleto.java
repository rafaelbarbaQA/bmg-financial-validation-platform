package com.automation.features.pagamento.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PagamentoBoleto {

    private final BoletoParaPagamento boleto;
    private final ModalidadePagamento modalidade;
    private final LocalDate dataPagamento;
    private final BigDecimal valorPagamento;

    private PagamentoBoleto(
            BoletoParaPagamento boleto,
            ModalidadePagamento modalidade,
            LocalDate dataPagamento,
            BigDecimal valorPagamento
    ) {

        validar(
                boleto,
                modalidade,
                dataPagamento,
                valorPagamento
        );

        this.boleto = boleto;
        this.modalidade = modalidade;
        this.dataPagamento = dataPagamento;
        this.valorPagamento = valorPagamento;
    }

    public static PagamentoBoleto total(
            BoletoParaPagamento boleto,
            LocalDate dataPagamento
    ) {

        if (boleto == null) {
            throw new IllegalArgumentException(
                    "O boleto não pode ser nulo."
            );
        }

        return new PagamentoBoleto(
                boleto,
                ModalidadePagamento.TOTAL,
                dataPagamento,
                boleto.getValorBoleto()
        );
    }

    public static PagamentoBoleto parcial(
            BoletoParaPagamento boleto,
            LocalDate dataPagamento,
            BigDecimal valorPagamento
    ) {

        return new PagamentoBoleto(
                boleto,
                ModalidadePagamento.PARCIAL,
                dataPagamento,
                valorPagamento
        );
    }

    private void validar(
            BoletoParaPagamento boleto,
            ModalidadePagamento modalidade,
            LocalDate dataPagamento,
            BigDecimal valorPagamento
    ) {

        if (boleto == null) {
            throw new IllegalArgumentException(
                    "O boleto não pode ser nulo."
            );
        }

        if (modalidade == null) {
            throw new IllegalArgumentException(
                    "A modalidade de pagamento não pode ser nula."
            );
        }

        if (dataPagamento == null) {
            throw new IllegalArgumentException(
                    "A data do pagamento não pode ser nula."
            );
        }

        if (valorPagamento == null
                || valorPagamento.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "O valor do pagamento deve ser maior que zero."
            );
        }

        if (modalidade == ModalidadePagamento.TOTAL
                && valorPagamento.compareTo(
                        boleto.getValorBoleto()
                ) != 0) {

            throw new IllegalArgumentException(
                    "Na modalidade TOTAL, o valor do pagamento deve ser "
                            + "igual ao valor total do boleto."
            );
        }

        if (modalidade == ModalidadePagamento.PARCIAL) {

            if (boleto.getTipoBoleto()
                    != TipoBoleto.FATURA_MENSAL) {

                throw new IllegalArgumentException(
                        "Pagamento parcial é permitido somente "
                                + "para boleto de fatura mensal, tipo 7."
                );
            }

            if (valorPagamento.compareTo(
                    boleto.getValorBoleto()
            ) >= 0) {

                throw new IllegalArgumentException(
                        "Na modalidade PARCIAL, o valor do pagamento "
                                + "deve ser menor que o valor total do boleto."
                );
            }
        }

        if (boleto.getTipoBoleto()
                == TipoBoleto.QUITACAO
                && modalidade != ModalidadePagamento.TOTAL) {

            throw new IllegalArgumentException(
                    "O boleto de quitação, tipo 9, "
                            + "somente permite pagamento total."
            );
        }
    }

    public BoletoParaPagamento getBoleto() {
        return boleto;
    }

    public ModalidadePagamento getModalidade() {
        return modalidade;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public BigDecimal getValorPagamento() {
        return valorPagamento;
    }

    public long getIdBoleto() {
        return boleto.getIdBoleto();
    }

    public long getIdConta() {
        return boleto.getIdConta();
    }

    public TipoBoleto getTipoBoleto() {
        return boleto.getTipoBoleto();
    }

    public String getNossoNumero() {
        return boleto.getNossoNumero();
    }
}
