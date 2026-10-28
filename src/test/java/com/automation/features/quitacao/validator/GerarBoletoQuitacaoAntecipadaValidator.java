package com.automation.features.quitacao.validator;

import com.automation.features.quitacao.soap.response
        .GerarBoletoQuitacaoAntecipadaResponse;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import org.testng.Assert;

public class GerarBoletoQuitacaoAntecipadaValidator {

    @Step("Validar resposta da geração do boleto de quitação")
    public void validar(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        validarRespostaNaoNula(response);
        validarDescricaoRetorno(response);
        validarCodigoRetorno(response);
        validarIdBoleto(response);
        validarIdLog(response);
        registrarStatusRegistro(response);
    }

    @Step("Validar que a resposta não está nula")
    private void validarRespostaNaoNula(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response,
                "A resposta da geração do boleto está nula."
        );
    }

    @Step("Validar descrição de retorno preenchida")
    private void validarDescricaoRetorno(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        String descricaoRetorno =
                response.getDescricaoRetorno();

        Assert.assertNotNull(
                descricaoRetorno,
                "A descrição de retorno não foi informada."
        );

        Assert.assertFalse(
                descricaoRetorno.isBlank(),
                "A descrição de retorno foi informada vazia."
        );
    }

    @Step("Validar código de retorno igual a zero")
    private void validarCodigoRetorno(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        int codigoRetorno =
                response.getCodRetorno();

        String descricaoRetorno =
                response.getDescricaoRetorno();

        Assert.assertEquals(
                codigoRetorno,
                0,
                "Erro ao gerar boleto: "
                        + descricaoRetorno
        );
    }

    @Step("Validar identificador do boleto maior que zero")
    private void validarIdBoleto(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        Assert.assertTrue(
                response.getIdBoleto() > 0,
                "O IdBoleto retornado é inválido."
        );
    }

    @Step("Validar identificador do log preenchido")
    private void validarIdLog(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        String idLog =
                response.getIdLog();

        Assert.assertNotNull(
                idLog,
                "O IDLog não foi retornado."
        );

        Assert.assertFalse(
                idLog.isBlank(),
                "O IDLog foi retornado vazio."
        );
    }

    @Step("Registrar status de registro externo do boleto")
    private void registrarStatusRegistro(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        boolean registrado =
                response.isRegistrado();

        String mensagemRegistro =
                obterMensagemRegistro(response);

        String evidencia =
                "FlagRegistrado: "
                        + registrado
                        + System.lineSeparator()
                        + "MensagemRegistro: "
                        + mensagemRegistro
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Regra aplicada: o registro externo "
                        + "não é critério obrigatório para "
                        + "a geração do boleto neste cenário.";

        Allure.addAttachment(
                "Status do registro externo",
                "text/plain",
                evidencia
        );

        if (registrado) {

            Allure.step(
                    "Boleto registrado externamente"
            );

            return;
        }

        Allure.step(
                "Boleto não registrado externamente "
                        + "- condição informativa e permitida"
        );
    }

    private String obterMensagemRegistro(
            GerarBoletoQuitacaoAntecipadaResponse response
    ) {

        String mensagem =
                response.getRetornoMensagemRegistro();

        if (mensagem == null || mensagem.isBlank()) {
            return "Não informada";
        }

        return mensagem.trim();
    }
}
