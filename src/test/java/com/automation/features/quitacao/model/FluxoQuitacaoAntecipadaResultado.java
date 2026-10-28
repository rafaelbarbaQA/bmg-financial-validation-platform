package com.automation.features.quitacao.model;

import com.automation.features.quitacao.soap.response
        .GerarBoletoQuitacaoAntecipadaResponse;

import com.automation.features.quitacao.soap.response
        .SimularQuitacaoAntecipadaResponse;

public final class FluxoQuitacaoAntecipadaResultado {

    private final SimularQuitacaoAntecipadaResponse respostaSimulacao;

    private final GerarBoletoQuitacaoAntecipadaResponse respostaGeracao;

    public FluxoQuitacaoAntecipadaResultado(
            SimularQuitacaoAntecipadaResponse respostaSimulacao,
            GerarBoletoQuitacaoAntecipadaResponse respostaGeracao
    ) {

        if (respostaSimulacao == null) {
            throw new IllegalArgumentException(
                    "A resposta da simulação não pode ser nula."
            );
        }

        if (respostaGeracao == null) {
            throw new IllegalArgumentException(
                    "A resposta da geração não pode ser nula."
            );
        }

        this.respostaSimulacao =
                respostaSimulacao;

        this.respostaGeracao =
                respostaGeracao;
    }

    public SimularQuitacaoAntecipadaResponse getRespostaSimulacao() {
        return respostaSimulacao;
    }

    public GerarBoletoQuitacaoAntecipadaResponse getRespostaGeracao() {
        return respostaGeracao;
    }

    public long getIdBoleto() {
        return respostaGeracao.getIdBoleto();
    }
}
