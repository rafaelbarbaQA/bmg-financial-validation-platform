package com.automation.features.quitacao.service;

import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.core.soap.SoapGateway;
import com.automation.features.quitacao.soap.QuitacaoSoapConfig;

import com.automation.features.quitacao.soap.request
        .GerarBoletoQuitacaoAntecipada;

import com.automation.features.quitacao.soap.response
        .GerarBoletoQuitacaoAntecipadaResponse;

import io.qameta.allure.Step;

public class GerarBoletoQuitacaoAntecipadaService {

    private final SoapGateway soapGateway;

    public GerarBoletoQuitacaoAntecipadaService() {

        this(
                new SoapGateway()
        );
    }

    public GerarBoletoQuitacaoAntecipadaService(
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

    @Step("Solicitar geração de boleto de quitação antecipada via SOAP")
    public GerarBoletoQuitacaoAntecipadaResponse executar(
            long idConta,
            String tipoCorrespondencia
    ) throws Exception {

        validarDadosEntrada(
                idConta,
                tipoCorrespondencia
        );

        GerarBoletoQuitacaoAntecipada request =
                new GerarBoletoQuitacaoAntecipada();

        request.setIdConta(
                String.valueOf(idConta)
        );

        request.setTipoCorrespondencia(
                tipoCorrespondencia.trim()
        );

        String xmlRequest =
                request.createGerarBoletoQuitacaoAntecipada();

        String endpoint =
                soapGateway.montarEndpoint(
                        QuitacaoSoapConfig.ENDPOINT_CONTA
                );

        EffectfulExecutionGuard.exigirAutorizacao();

        String xmlResponse =
                soapGateway.enviarRequisicaoSoap(
                        xmlRequest,
                        endpoint,
                        QuitacaoSoapConfig.SOAP_ACTION_GERAR_BOLETO_QUITACAO_ANTECIPADA
                );

        return new GerarBoletoQuitacaoAntecipadaResponse(
                xmlResponse
        );
    }

    private void validarDadosEntrada(
            long idConta,
            String tipoCorrespondencia
    ) {

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }

        if (tipoCorrespondencia == null
                || tipoCorrespondencia.isBlank()) {

            throw new IllegalArgumentException(
                    "O TipoCorrespondencia deve ser informado."
            );
        }
    }
}
