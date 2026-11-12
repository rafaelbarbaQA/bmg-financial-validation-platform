package com.automation.features.quitacao.validator;

import com.automation.features.quitacao.soap.response.SimularQuitacaoAntecipadaResponse;

import io.qameta.allure.Step;

import org.testng.Assert;

import java.math.BigDecimal;

public class SimularQuitacaoAntecipadaValidator {

    private static final int CODIGO_RETORNO_SUCESSO = 0;
    private static final String DESCRICAO_RETORNO_SUCESSO = "Ok";

    @Step("Validar resposta da simulação de quitação")
    public void validar(
            SimularQuitacaoAntecipadaResponse response
    ) {

        validarRespostaPreenchida(response);

        validarCodigoRetorno(response);

        validarDescricaoRetorno(response);

        validarTotalAntecipacao(response);

        validarSaldoFaturaAnterior(response);
    }

    @Step("Validar que a resposta não está nula")
    private void validarRespostaPreenchida(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response,
                "A resposta da simulação não pode ser nula."
        );
    }

    @Step("Validar código de retorno igual a zero")
    private void validarCodigoRetorno(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertEquals(
                response.getCodRetorno(),
                CODIGO_RETORNO_SUCESSO,
                "Erro ao simular quitação: "
                        + response.getDescricaoRetorno()
        );
    }

    @Step("Validar descrição de retorno")
    private void validarDescricaoRetorno(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response.getDescricaoRetorno(),
                "A descrição de retorno não pode ser nula."
        );

        Assert.assertEquals(
                response.getDescricaoRetorno(),
                DESCRICAO_RETORNO_SUCESSO,
                "A descrição da simulação está diferente do esperado."
        );
    }

    @Step("Validar total da antecipação maior que zero")
    private void validarTotalAntecipacao(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response.getTotalAntecipacao(),
                "O TotalAntecipacao não pode ser nulo."
        );

        Assert.assertTrue(
                response
                        .getTotalAntecipacao()
                        .compareTo(BigDecimal.ZERO) > 0,
                "O TotalAntecipacao deve ser maior que zero."
        );
    }

    @Step("Validar saldo da fatura anterior não negativo")
    private void validarSaldoFaturaAnterior(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response.getSaldoFaturaAnterior(),
                "O SaldoFaturaAnterior não pode ser nulo."
        );

        Assert.assertTrue(
                response
                        .getSaldoFaturaAnterior()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "O SaldoFaturaAnterior não pode ser negativo."
        );
    }
}
