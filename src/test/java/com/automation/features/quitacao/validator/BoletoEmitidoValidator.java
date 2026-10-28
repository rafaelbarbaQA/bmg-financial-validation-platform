package com.automation.features.quitacao.validator;

import com.automation.features.quitacao.model.BoletoEmitido;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import org.testng.Assert;

import java.util.Optional;

public class BoletoEmitidoValidator {

    @Step("Validar boleto gerado na base de dados")
    public void validar(
            Optional<BoletoEmitido> resultadoConsulta,
            long idContaEsperado,
            long idBoletoEsperado
    ) {

        validarResultadoNaoNulo(
                resultadoConsulta
        );

        validarBoletoEncontrado(
                resultadoConsulta,
                idContaEsperado,
                idBoletoEsperado
        );
    }

    @Step("Validar que o resultado da consulta não está nulo")
    private void validarResultadoNaoNulo(
            Optional<BoletoEmitido> resultadoConsulta
    ) {

        Assert.assertNotNull(
                resultadoConsulta,
                "O resultado da consulta do boleto está nulo."
        );
    }

    @Step("Validar que o boleto foi localizado")
    private void validarBoletoEncontrado(
            Optional<BoletoEmitido> resultadoConsulta,
            long idContaEsperado,
            long idBoletoEsperado
    ) {

        Assert.assertTrue(
                resultadoConsulta.isPresent(),
                "O boleto não foi localizado na tabela "
                        + "BoletosEmitidos. "
                        + "IdConta: "
                        + idContaEsperado
                        + " | IdBoleto: "
                        + idBoletoEsperado
        );

        BoletoEmitido boletoEmitido =
                resultadoConsulta.get();

        validarIdConta(
                boletoEmitido,
                idContaEsperado
        );

        validarIdBoleto(
                boletoEmitido,
                idBoletoEsperado
        );

        anexarConfirmacao(
                boletoEmitido
        );
    }

    @Step("Validar IdConta do boleto")
    private void validarIdConta(
            BoletoEmitido boletoEmitido,
            long idContaEsperado
    ) {

        Assert.assertEquals(
                boletoEmitido.getIdConta(),
                idContaEsperado,
                "O IdConta encontrado no banco está diferente "
                        + "do utilizado no teste."
        );
    }

    @Step("Validar IdBoleto retornado pelo SOAP")
    private void validarIdBoleto(
            BoletoEmitido boletoEmitido,
            long idBoletoEsperado
    ) {

        Assert.assertEquals(
                boletoEmitido.getIdBoleto(),
                idBoletoEsperado,
                "O IdBoleto encontrado no banco está diferente "
                        + "do retornado pelo SOAP."
        );
    }

    @Step("Confirmar geração e persistência do boleto")
    private void anexarConfirmacao(
            BoletoEmitido boletoEmitido
    ) {

        String evidencia =
                "Boleto gerado e localizado com sucesso na base."
                        + System.lineSeparator()
                        + "IdConta: "
                        + boletoEmitido.getIdConta()
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + boletoEmitido.getIdBoleto();

        Allure.step(
                "Boleto gerado e localizado com sucesso na base"
        );

        Allure.addAttachment(
                "Confirmação da geração do boleto",
                "text/plain",
                evidencia
        );
    }
}
