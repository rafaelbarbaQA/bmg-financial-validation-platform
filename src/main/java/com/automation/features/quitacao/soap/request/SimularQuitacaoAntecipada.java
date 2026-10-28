package com.automation.features.quitacao.soap.request;

public class SimularQuitacaoAntecipada {

    private String idConta;

    public void setIdConta(String idConta) {
        this.idConta = idConta;
    }

    public String createSimularQuitacaoAntecipada() {

        if (idConta == null || idConta.isBlank()) {
            throw new IllegalArgumentException(
                    "IdConta não foi informado para a simulação."
            );
        }

        return "<soapenv:Envelope "
                + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:con=\"http://conductor.com.br/\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <con:SimularQuitacaoAntecipada>\n"
                + "         <con:req>\n"
                + "            <con:IdConta>"
                + idConta
                + "</con:IdConta>\n"
                + "         </con:req>\n"
                + "      </con:SimularQuitacaoAntecipada>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
    }
}

