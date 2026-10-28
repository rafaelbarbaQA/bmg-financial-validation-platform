package com.automation.tests.quitacao;

import com.automation.features.quitacao.service
        .SimularQuitacaoAntecipadaService;

import com.automation.features.quitacao.validator
        .SimularQuitacaoAntecipadaValidator;

import com.automation.features.quitacao.soap.response
        .SimularQuitacaoAntecipadaResponse;

import com.automation.features.quitacao.testdata
        .QuitacaoTestConfig;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import org.testng.annotations.Test;

@Epic("QuitaÃ§Ã£o")
@Feature("QuitaÃ§Ã£o Antecipada")
@Owner("QA Automation")
public class SimularQuitacaoAntecipadaTest {

    @Test(
            description =
                    "Deve simular a quitaÃ§Ã£o antecipada com sucesso",
            groups = {
                    "integration",
                    "soap",
                    "quitacao",
                    "simulacao",
                    "readonly"
            }
    )
    @Story("SimulaÃ§Ã£o de quitaÃ§Ã£o antecipada")
    @Severity(SeverityLevel.CRITICAL)
    @Description(
            "Realiza a simulaÃ§Ã£o de quitaÃ§Ã£o antecipada via SOAP "
                    + "e valida os dados retornados pelo serviÃ§o."
    )
    public void deveSimularQuitacaoAntecipadaComSucesso()
            throws Exception {

        long idConta =
                QuitacaoTestConfig
                        .getIdContaSmoke();

        SimularQuitacaoAntecipadaService simulacaoService =
                new SimularQuitacaoAntecipadaService();

        SimularQuitacaoAntecipadaValidator simulacaoValidator =
                new SimularQuitacaoAntecipadaValidator();

        SimularQuitacaoAntecipadaResponse response =
                simulacaoService.executar(
                        idConta
                );

        System.out.println();
        System.out.println("========== SIMULACAO QUITACAO ==========");
        System.out.println("CodRetorno..............: " + response.getCodRetorno());
        System.out.println("Descricao...............: " + response.getDescricaoRetorno());
        System.out.println("TransacoesPeriodo.......: " + response.getTransacoesDoPeriodo());
        System.out.println("TransacoesAntecipadas...: " + response.getTransacoesAntecipadas());
        System.out.println("AjusteCredito...........: " + response.getAjusteCredito());
        System.out.println("AjusteDebito............: " + response.getAjusteDebito());
        System.out.println("Encargos................: " + response.getEncargos());
        System.out.println("IOF.....................: " + response.getIof());
        System.out.println("SaldoFaturaAnterior.....: " + response.getSaldoFaturaAnterior());
        System.out.println("TotalDesconto...........: " + response.getTotalDesconto());
        System.out.println("AbatimentoJuros.........: " + response.getAbatimentoJuros());
        System.out.println("TotalAntecipacao........: " + response.getTotalAntecipacao());
        System.out.println("========================================");

        simulacaoValidator.validar(
                response
        );
    }
}
