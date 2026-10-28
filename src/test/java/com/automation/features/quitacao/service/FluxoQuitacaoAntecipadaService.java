package com.automation.features.quitacao.service;

import com.automation.features.quitacao.validator.GerarBoletoQuitacaoAntecipadaValidator;

import com.automation.features.quitacao.validator.SimularQuitacaoAntecipadaValidator;

import com.automation.features.quitacao.soap.response
        .GerarBoletoQuitacaoAntecipadaResponse;

import com.automation.features.quitacao.soap.response
        .SimularQuitacaoAntecipadaResponse;

import com.automation.features.quitacao.model
        .FluxoQuitacaoAntecipadaResultado;

import io.qameta.allure.Step;

public class FluxoQuitacaoAntecipadaService {

    private final SimularQuitacaoAntecipadaService simulacaoService;

    private final GerarBoletoQuitacaoAntecipadaService geracaoService;

    private final SimularQuitacaoAntecipadaValidator simulacaoValidator;

    private final GerarBoletoQuitacaoAntecipadaValidator geracaoValidator;

    public FluxoQuitacaoAntecipadaService() {

        this(
                new SimularQuitacaoAntecipadaService(),
                new GerarBoletoQuitacaoAntecipadaService(),
                new SimularQuitacaoAntecipadaValidator(),
                new GerarBoletoQuitacaoAntecipadaValidator()
        );
    }

    public FluxoQuitacaoAntecipadaService(
            SimularQuitacaoAntecipadaService simulacaoService,
            GerarBoletoQuitacaoAntecipadaService geracaoService,
            SimularQuitacaoAntecipadaValidator simulacaoValidator,
            GerarBoletoQuitacaoAntecipadaValidator geracaoValidator
    ) {

        if (simulacaoService == null) {
            throw new IllegalArgumentException(
                    "O serviço de simulação não pode ser nulo."
            );
        }

        if (geracaoService == null) {
            throw new IllegalArgumentException(
                    "O serviço de geração não pode ser nulo."
            );
        }

        if (simulacaoValidator == null) {
            throw new IllegalArgumentException(
                    "O validator da simulação não pode ser nulo."
            );
        }

        if (geracaoValidator == null) {
            throw new IllegalArgumentException(
                    "O validator da geração não pode ser nulo."
            );
        }

        this.simulacaoService =
                simulacaoService;

        this.geracaoService =
                geracaoService;

        this.simulacaoValidator =
                simulacaoValidator;

        this.geracaoValidator =
                geracaoValidator;
    }

    @Step("Executar e validar fluxo SOAP de quitação antecipada")
    public FluxoQuitacaoAntecipadaResultado executar(
            long idConta,
            String tipoCorrespondencia
    ) throws Exception {

        SimularQuitacaoAntecipadaResponse respostaSimulacao =
                simulacaoService.executar(
                        idConta
                );

        simulacaoValidator.validar(
                respostaSimulacao
        );

        GerarBoletoQuitacaoAntecipadaResponse respostaGeracao =
                geracaoService.executar(
                        idConta,
                        tipoCorrespondencia
                );

        geracaoValidator.validar(
                respostaGeracao
        );

        return new FluxoQuitacaoAntecipadaResultado(
                respostaSimulacao,
                respostaGeracao
        );
    }
}
