package com.automation.features.quitacao.soap;

public final class QuitacaoSoapConfig {

    private static final String SOAP_NAMESPACE =
            "http://conductor.com.br/";

    public static final String ENDPOINT_CONTA =
            "Conta.asmx";

    public static final String SOAP_ACTION_SIMULAR_QUITACAO_ANTECIPADA =
            SOAP_NAMESPACE
                    + "SimularQuitacaoAntecipada";

    public static final String SOAP_ACTION_GERAR_BOLETO_QUITACAO_ANTECIPADA =
            SOAP_NAMESPACE
                    + "GerarBoletoQuitacaoAntecipada";

    private QuitacaoSoapConfig() {
        // Impede instanciação.
    }
}
