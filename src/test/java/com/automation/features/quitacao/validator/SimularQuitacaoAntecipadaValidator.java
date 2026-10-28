package com.automation.features.quitacao.validator;

import com.automation.features.quitacao.soap.response.SimularQuitacaoAntecipadaResponse;

import io.qameta.allure.Step;

import org.testng.Assert;

import java.math.BigDecimal;

public class SimularQuitacaoAntecipadaValidator {

    private static final int CODIGO_RETORNO_SUCESSO = 0;
    private static final String DESCRICAO_RETORNO_SUCESSO = "Ok";

    @Step("Validar resposta da simulaÃ§Ã£o de quitaÃ§Ã£o")
    public void validar(
            SimularQuitacaoAntecipadaResponse response
    ) {

        validarRespostaPreenchida(response);

        validarCodigoRetorno(response);

        validarDescricaoRetorno(response);

        validarTotalAntecipacao(response);

        validarSaldoFaturaAnterior(response);
    }

    @Step("Validar que a resposta nÃ£o estÃ¡ nula")
    private void validarRespostaPreenchida(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response,
                "A resposta da simulaÃ§Ã£o nÃ£o pode ser nula."
        );
    }

    @Step("Validar cÃ³digo de retorno igual a zero")
    private void validarCodigoRetorno(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertEquals(
                response.getCodRetorno(),
                CODIGO_RETORNO_SUCESSO,
                "Erro ao simular quitaÃ§Ã£o: "
                        + response.getDescricaoRetorno()
        );
    }

    @Step("Validar descriÃ§Ã£o de retorno")
    private void validarDescricaoRetorno(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response.getDescricaoRetorno(),
                "A descriÃ§Ã£o de retorno nÃ£o pode ser nula."
        );

        Assert.assertEquals(
                response.getDescricaoRetorno(),
                DESCRICAO_RETORNO_SUCESSO,
                "A descriÃ§Ã£o da simulaÃ§Ã£o estÃ¡ diferente do esperado."
        );
    }

    @Step("Validar total da antecipaÃ§Ã£o maior que zero")
    private void validarTotalAntecipacao(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response.getTotalAntecipacao(),
                "O TotalAntecipacao nÃ£o pode ser nulo."
        );

        Assert.assertTrue(
                response
                        .getTotalAntecipacao()
                        .compareTo(BigDecimal.ZERO) > 0,
                "O TotalAntecipacao deve ser maior que zero."
        );
    }

    @Step("Validar saldo da fatura anterior nÃ£o negativo")
    private void validarSaldoFaturaAnterior(
            SimularQuitacaoAntecipadaResponse response
    ) {

        Assert.assertNotNull(
                response.getSaldoFaturaAnterior(),
                "O SaldoFaturaAnterior nÃ£o pode ser nulo."
        );

        Assert.assertTrue(
                response
                        .getSaldoFaturaAnterior()
                        .compareTo(BigDecimal.ZERO) >= 0,
                "O SaldoFaturaAnterior nÃ£o pode ser negativo."
        );
    }
}
