package com.automation.tests.quitacao;

import com.automation.core.execution
        .EffectfulExecutionGuard;

import com.automation.features.quitacao.service
        .GerarBoletoQuitacaoAntecipadaService;

import com.automation.features.quitacao.validator
        .GerarBoletoQuitacaoAntecipadaValidator;

import com.automation.features.quitacao.soap.response
        .GerarBoletoQuitacaoAntecipadaResponse;

import com.automation.features.quitacao.testdata
        .QuitacaoTestConfig;

import org.testng.annotations.Test;

public class GerarBoletoQuitacaoAntecipadaTest {

    @Test(
            groups = {
                    "integration",
                    "soap",
                    "quitacao",
                    "effectful"
            }
    )
    public void deveGerarBoletoQuitacaoAntecipadaComSucesso()
            throws Exception {

        long idConta =
                QuitacaoTestConfig
                        .getIdContaSmoke();

        String tipoCorrespondencia =
                QuitacaoTestConfig
                        .getTipoCorrespondencia();

        EffectfulExecutionGuard
                .exigirAutorizacao();

        GerarBoletoQuitacaoAntecipadaService geracaoService =
                new GerarBoletoQuitacaoAntecipadaService();

        GerarBoletoQuitacaoAntecipadaValidator geracaoValidator =
                new GerarBoletoQuitacaoAntecipadaValidator();

        GerarBoletoQuitacaoAntecipadaResponse response =
                geracaoService.executar(
                        idConta,
                        tipoCorrespondencia
                );

        geracaoValidator.validar(
                response
        );
    }
}