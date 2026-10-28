package com.automation.core.soap;

import com.automation.core.config.ConfigReader;

import io.qameta.allure.Step;

import java.io.IOException;

public final class SoapGateway {

    private final String wsUrl;

    public SoapGateway() {

        this(
                ConfigReader.getProperty(
                        "WS_URL"
                )
        );
    }

    public SoapGateway(
            String wsUrl
    ) {

        if (wsUrl == null || wsUrl.isBlank()) {
            throw new IllegalStateException(
                    "A URL base dos serviços SOAP "
                            + "não foi configurada."
            );
        }

        this.wsUrl =
                wsUrl.trim();
    }

    @Step("Enviar requisição SOAP")
    public String enviarRequisicaoSoap(
            String xmlRequest,
            String endpoint,
            String soapAction
    ) throws IOException {

        validarXmlRequest(
                xmlRequest
        );

        validarEndpointCompleto(
                endpoint
        );

        try (
                SoapClient soapClient =
                        new SoapClient(endpoint)
        ) {

            return soapClient.sendRequest(
                    xmlRequest,
                    soapAction
            );
        }
    }

    @Step("Montar endpoint do serviço SOAP")
    public String montarEndpoint(
            String endpointServico
    ) {

        if (endpointServico == null
                || endpointServico.isBlank()) {

            throw new IllegalArgumentException(
                    "O endpoint do serviço não foi informado."
            );
        }

        String endpointTratado =
                endpointServico.trim();

        if (wsUrl.endsWith("/")
                && endpointTratado.startsWith("/")) {

            return wsUrl
                    + endpointTratado.substring(1);
        }

        if (!wsUrl.endsWith("/")
                && !endpointTratado.startsWith("/")) {

            return wsUrl
                    + "/"
                    + endpointTratado;
        }

        return wsUrl + endpointTratado;
    }

    private void validarXmlRequest(
            String xmlRequest
    ) {

        if (xmlRequest == null
                || xmlRequest.isBlank()) {

            throw new IllegalArgumentException(
                    "O XML da requisição SOAP "
                            + "não pode ser nulo ou vazio."
            );
        }
    }

    private void validarEndpointCompleto(
            String endpoint
    ) {

        if (endpoint == null
                || endpoint.isBlank()) {

            throw new IllegalArgumentException(
                    "O endpoint SOAP completo "
                            + "não pode ser nulo ou vazio."
            );
        }
    }
}
