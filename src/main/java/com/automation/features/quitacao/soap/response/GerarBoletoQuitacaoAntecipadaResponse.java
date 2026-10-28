package com.automation.features.quitacao.soap.response;

import com.automation.core.xml.XmlHelper;

import org.w3c.dom.Document;

import java.util.Locale;

public class GerarBoletoQuitacaoAntecipadaResponse {

    private final Document document;

    public GerarBoletoQuitacaoAntecipadaResponse(
            String xmlResponse
    ) {

        if (xmlResponse == null || xmlResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "O XML de resposta não pode ser nulo ou vazio."
            );
        }

        this.document =
                XmlHelper.parsear(xmlResponse);

        validarSoapFault();
    }

    public long getIdBoleto() {
        return obterValorLong("IdBoleto");
    }

    public int getCodRetorno() {
        return obterValorInteiro("CodRetorno");
    }

    public String getDescricaoRetorno() {
        return obterTexto("DescricaoRetorno");
    }

    public String getIdLog() {
        return obterTexto("IDLog");
    }

    public boolean isRegistrado() {
        return obterValorBooleano("FlagRegistrado");
    }

    public String getRetornoMensagemRegistro() {

        if (!XmlHelper.possuiElementoPorNomeLocal(
                document,
                "RetornoMensagemRegistro"
        )) {
            return "";
        }

        return obterTexto("RetornoMensagemRegistro");
    }

    private String obterTexto(
            String nomeElemento
    ) {

        return XmlHelper.obterValorPorNomeLocal(
                document,
                nomeElemento
        );
    }

    private long obterValorLong(
            String nomeElemento
    ) {

        String valor = obterTexto(nomeElemento);

        try {
            return Long.parseLong(valor);

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O elemento "
                            + nomeElemento
                            + " não possui um número válido: "
                            + valor,
                    exception
            );
        }
    }

    private int obterValorInteiro(
            String nomeElemento
    ) {

        String valor = obterTexto(nomeElemento);

        try {
            return Integer.parseInt(valor);

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O elemento "
                            + nomeElemento
                            + " não possui um número inteiro válido: "
                            + valor,
                    exception
            );
        }
    }

    private boolean obterValorBooleano(
            String nomeElemento
    ) {

        String valor =
                obterTexto(nomeElemento)
                        .toLowerCase(Locale.ROOT);

        switch (valor) {
            case "true":
            case "1":
                return true;

            case "false":
            case "0":
                return false;

            default:
                throw new IllegalStateException(
                        "O elemento "
                                + nomeElemento
                                + " não possui um booleano válido: "
                                + valor
                );
        }
    }

    private void validarSoapFault() {

        if (!XmlHelper.possuiElementoPorNomeLocal(
                document,
                "Fault"
        )) {
            return;
        }

        String mensagemFault =
                obterMensagemSoapFault();

        throw new IllegalStateException(
                "O serviço retornou um SOAP Fault: "
                        + mensagemFault
        );
    }

    private String obterMensagemSoapFault() {

        if (XmlHelper.possuiElementoPorNomeLocal(
                document,
                "faultstring"
        )) {
            return obterTexto("faultstring");
        }

        if (XmlHelper.possuiElementoPorNomeLocal(
                document,
                "Text"
        )) {
            return obterTexto("Text");
        }

        return "SOAP Fault sem mensagem detalhada.";
    }
}

