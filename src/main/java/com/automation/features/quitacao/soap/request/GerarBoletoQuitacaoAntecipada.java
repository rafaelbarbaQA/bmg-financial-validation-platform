package com.automation.features.quitacao.soap.request;

public class GerarBoletoQuitacaoAntecipada {

    private String idConta;
    private String tipoCorrespondencia;

    public void setIdConta(String idConta) {
        this.idConta = idConta;
    }

    public void setTipoCorrespondencia(String tipoCorrespondencia) {
        this.tipoCorrespondencia = tipoCorrespondencia;
    }

    public String createGerarBoletoQuitacaoAntecipada() {

        String idContaValidado =
                validarCampoObrigatorio(
                        idConta,
                        "IdConta"
                );

        String tipoCorrespondenciaValidado =
                validarCampoObrigatorio(
                        tipoCorrespondencia,
                        "TipoCorrespondencia"
                );

        return "<soapenv:Envelope "
                + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:con=\"http://conductor.com.br/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <con:GerarBoletoQuitacaoAntecipada>\n"
                + "         <con:req>\n"
                + "            <con:IdConta>"
                + idContaValidado
                + "</con:IdConta>\n"
                + "            <con:TipoCorrespondencia>"
                + tipoCorrespondenciaValidado
                + "</con:TipoCorrespondencia>\n"
                + "         </con:req>\n"
                + "      </con:GerarBoletoQuitacaoAntecipada>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }

    private String validarCampoObrigatorio(
            String valor,
            String nomeCampo
    )
    {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    nomeCampo + " não foi informado."
            );
        }

        return valor.trim();
    }
}

