package com.automation.features.pagamento.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class EventoExternoPagamento {

    private final long idEventoPagamento;
    private final int banco;
    private final LocalDate dataMovimento;
    private final LocalDate dataPagamento;
    private final String nossoNumero;
    private final BigDecimal valorPago;
    private final String cartao;
    private final int idAutorizacao;
    private final long idConta;
    private final int idOperacao;
    private final LocalDate dataEntradaPagamento;
    private final LocalDate dataVencimentoPadrao;
    private final LocalDate dataVencimentoReal;
    private final BigDecimal restituicaoSaque;
    private final int status;

    public EventoExternoPagamento(
            long idEventoPagamento,
            int banco,
            LocalDate dataMovimento,
            LocalDate dataPagamento,
            String nossoNumero,
            BigDecimal valorPago,
            String cartao,
            int idAutorizacao,
            long idConta,
            int idOperacao,
            LocalDate dataEntradaPagamento,
            LocalDate dataVencimentoPadrao,
            LocalDate dataVencimentoReal,
            BigDecimal restituicaoSaque,
            int status
    ) {

        validar(
                idEventoPagamento,
                banco,
                dataMovimento,
                dataPagamento,
                nossoNumero,
                valorPago,
                cartao,
                idAutorizacao,
                idConta,
                idOperacao,
                dataEntradaPagamento,
                restituicaoSaque,
                status
        );

        this.idEventoPagamento = idEventoPagamento;
        this.banco = banco;
        this.dataMovimento = dataMovimento;
        this.dataPagamento = dataPagamento;
        this.nossoNumero = nossoNumero.trim();
        this.valorPago = valorPago;
        this.cartao = cartao.trim();
        this.idAutorizacao = idAutorizacao;
        this.idConta = idConta;
        this.idOperacao = idOperacao;
        this.dataEntradaPagamento = dataEntradaPagamento;
        this.dataVencimentoPadrao = dataVencimentoPadrao;
        this.dataVencimentoReal = dataVencimentoReal;
        this.restituicaoSaque = restituicaoSaque;
        this.status = status;
    }

    private void validar(
            long idEventoPagamento,
            int banco,
            LocalDate dataMovimento,
            LocalDate dataPagamento,
            String nossoNumero,
            BigDecimal valorPago,
            String cartao,
            int idAutorizacao,
            long idConta,
            int idOperacao,
            LocalDate dataEntradaPagamento,
            BigDecimal restituicaoSaque,
            int status
    ) {

        if (idEventoPagamento <= 0) {
            throw new IllegalArgumentException(
                    "O IdEventoPagamento deve ser "
                            + "maior que zero."
            );
        }

        if (banco <= 0) {
            throw new IllegalArgumentException(
                    "O código do banco deve ser "
                            + "maior que zero."
            );
        }

        if (dataMovimento == null) {
            throw new IllegalArgumentException(
                    "A DataMovimento não pode ser nula."
            );
        }

        if (dataPagamento == null) {
            throw new IllegalArgumentException(
                    "A DataPagamento não pode ser nula."
            );
        }

        if (nossoNumero == null
                || nossoNumero.isBlank()) {

            throw new IllegalArgumentException(
                    "O NossoNumero não pode ser vazio."
            );
        }

        String nossoNumeroNormalizado =
                nossoNumero.trim();

        if (!nossoNumeroNormalizado.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "O NossoNumero deve conter "
                            + "somente números."
            );
        }

        if (nossoNumeroNormalizado.length() > 20) {
            throw new IllegalArgumentException(
                    "O NossoNumero deve possuir "
                            + "no máximo 20 dígitos."
            );
        }

        if (valorPago == null
                || valorPago.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            throw new IllegalArgumentException(
                    "O ValorPago deve ser maior que zero."
            );
        }

        if (cartao == null
                || cartao.isBlank()) {

            throw new IllegalArgumentException(
                    "O Cartao não pode ser vazio."
            );
        }

        if (cartao.trim().length() > 16) {
            throw new IllegalArgumentException(
                    "O Cartao deve possuir no máximo "
                            + "16 caracteres."
            );
        }

        if (idAutorizacao <= 0) {
            throw new IllegalArgumentException(
                    "O IdAutorizacao deve ser "
                            + "maior que zero."
            );
        }

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }

        if (idOperacao <= 0) {
            throw new IllegalArgumentException(
                    "O IdOperacao deve ser maior que zero."
            );
        }

        if (dataEntradaPagamento == null) {
            throw new IllegalArgumentException(
                    "A DataEntradaPagamento não pode "
                            + "ser nula."
            );
        }

        if (restituicaoSaque == null
                || restituicaoSaque.compareTo(
                        BigDecimal.ZERO
                ) < 0) {

            throw new IllegalArgumentException(
                    "A RestituicaoSaque não pode "
                            + "ser negativa."
            );
        }

        if (status < 0) {
            throw new IllegalArgumentException(
                    "O Status não pode ser negativo."
            );
        }
    }

    public long getIdEventoPagamento() {
        return idEventoPagamento;
    }

    public int getBanco() {
        return banco;
    }

    public LocalDate getDataMovimento() {
        return dataMovimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public String getNossoNumero() {
        return nossoNumero;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public String getCartao() {
        return cartao;
    }

    public int getIdAutorizacao() {
        return idAutorizacao;
    }

    public long getIdConta() {
        return idConta;
    }

    public int getIdOperacao() {
        return idOperacao;
    }

    public LocalDate getDataEntradaPagamento() {
        return dataEntradaPagamento;
    }

    public LocalDate getDataVencimentoPadrao() {
        return dataVencimentoPadrao;
    }

    public LocalDate getDataVencimentoReal() {
        return dataVencimentoReal;
    }

    public BigDecimal getRestituicaoSaque() {
        return restituicaoSaque;
    }

    public int getStatus() {
        return status;
    }
}