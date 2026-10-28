package com.automation.features.quitacao.soap.response;

import com.automation.core.xml.XmlHelper;

import org.w3c.dom.Document;

import java.math.BigDecimal;

public class SimularQuitacaoAntecipadaResponse {

    private final Document document;

    public SimularQuitacaoAntecipadaResponse(
            String xmlResponse
    ) {

        this.document =
                XmlHelper.parsear(xmlResponse);

        validarSoapFault();
    }

    public int getCodRetorno() {

        String valor =
                obterValor("CodRetorno");

        try {
            return Integer.parseInt(valor);

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Valor inválido em CodRetorno: "
                            + valor,
                    exception
            );
        }
    }

    public String getDescricaoRetorno() {
        return obterValor("DescricaoRetorno");
    }

    public BigDecimal getTransacoesDoPeriodo() {
        return obterValorDecimal("TransacoesDoPeriodo");
    }

    public BigDecimal getTransacoesAntecipadas() {
        return obterValorDecimal("TransacoesAntecipadas");
    }

    public BigDecimal getAjusteCredito() {
        return obterValorDecimal("AjusteCredito");
    }

    public BigDecimal getAjusteDebito() {
        return obterValorDecimal("AjusteDebito");
    }

    public BigDecimal getEncargos() {
        return obterValorDecimal("Encargos");
    }

    public BigDecimal getIof() {
        return obterValorDecimal("IOF");
    }

    public BigDecimal getSaldoFaturaAnterior() {
        return obterValorDecimal("SaldoFaturaAnterior");
    }

    public BigDecimal getTotalDesconto() {
        return obterValorDecimal("TotalDesconto");
    }

    public BigDecimal getAbatimentoJuros() {
        return obterValorDecimal("AbatimentoJuros");
    }

    public BigDecimal getValorPagoMes() {
        return obterValorDecimal("ValorPagoMes");
    }

    public BigDecimal getTotalAntecipacao() {
        return obterValorDecimal("TotalAntecipacao");
    }

    private String obterValor(
            String nomeElemento
    ) {

        return XmlHelper.obterValorPorNomeLocal(
                document,
                nomeElemento
        );
    }

    private BigDecimal obterValorDecimal(
            String nomeElemento
    ) {

        String valor =
                obterValor(nomeElemento);

        /*
         * Alguns retornos do WS possuem campos numericos vazios.
         * Para valores monetarios/totalizadores, vazio representa zero.
         */
        if (valor == null
                || valor.trim().isEmpty()) {

            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(
                    valor.trim()
            );

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Valor decimal inválido no elemento "
                            + nomeElemento
                            + ": "
                            + valor,
                    exception
            );
        }
    }

    private void validarSoapFault() {

        if (XmlHelper.possuiElementoPorNomeLocal(
                document,
                "Fault"
        )) {

            String detalhes =
                    XmlHelper.obterValorPorNomeLocal(
                            document,
                            "Fault"
                    );

            throw new IllegalStateException(
                    "A resposta contém um SOAP Fault: "
                            + detalhes
            );
        }
    }
}

