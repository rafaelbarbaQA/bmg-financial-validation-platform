package com.automation.features.quitacao.service;

import com.automation.core.soap.SoapGateway;
import com.automation.features.quitacao.soap.QuitacaoSoapConfig;

import com.automation.features.quitacao.soap.request
        .SimularQuitacaoAntecipada;

import com.automation.features.quitacao.soap.response
        .SimularQuitacaoAntecipadaResponse;

import io.qameta.allure.Step;

public class SimularQuitacaoAntecipadaService {

    private final SoapGateway soapGateway;

    public SimularQuitacaoAntecipadaService() {

        this(
                new SoapGateway()
        );
    }

    public SimularQuitacaoAntecipadaService(
            SoapGateway soapGateway
    ) {

        if (soapGateway == null) {
            throw new IllegalArgumentException(
                    "O SoapGateway não pode ser nulo."
            );
        }

        this.soapGateway =
                soapGateway;
    }

    @Step("Executar simulação de quitação antecipada via SOAP")
    public SimularQuitacaoAntecipadaResponse executar(
            long idConta
    ) throws Exception {

        validarIdConta(
                idConta
        );

        SimularQuitacaoAntecipada request =
                new SimularQuitacaoAntecipada();

        request.setIdConta(
                String.valueOf(idConta)
        );

        String xmlRequest =
                request.createSimularQuitacaoAntecipada();

        String endpoint =
                soapGateway.montarEndpoint(
                        QuitacaoSoapConfig.ENDPOINT_CONTA
                );

        String xmlResponse =
                soapGateway.enviarRequisicaoSoap(
                        xmlRequest,
                        endpoint,
                        QuitacaoSoapConfig.SOAP_ACTION_SIMULAR_QUITACAO_ANTECIPADA
                );

        return new SimularQuitacaoAntecipadaResponse(
                xmlResponse
        );
    }

    private void validarIdConta(
            long idConta
    ) {

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }
    }
}
