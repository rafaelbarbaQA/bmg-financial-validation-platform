package com.automation.features.pagamento.validator;

import com.automation.features.pagamento.model.PagamentoExistente;
import com.automation.features.pagamento.model.StatusEventoPagamento;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import org.testng.Assert;

import java.util.Optional;

public final class PagamentoProcessadoValidator {

    @Step("Validar processamento do pagamento")
    public void validar(
            Optional<PagamentoExistente> pagamentoEncontrado,
            long idEventoPagamentoEsperado
    ) {

        validarParametros(
                pagamentoEncontrado,
                idEventoPagamentoEsperado
        );

        Allure.parameter(
                "idEventoPagamentoEsperado",
                idEventoPagamentoEsperado
        );

        if (pagamentoEncontrado.isEmpty()) {

            anexarPagamentoNaoEncontrado(
                    idEventoPagamentoEsperado
            );

            Assert.fail(
                    "O pagamento não foi encontrado em "
                            + "EventosExternosPagamentos. "
                            + "IdEventoPagamento: "
                            + idEventoPagamentoEsperado
                            + "."
            );
        }

        PagamentoExistente pagamento =
                pagamentoEncontrado.get();

        anexarPagamentoEncontrado(
                pagamento
        );

        Assert.assertEquals(
                pagamento.getIdEventoPagamento(),
                idEventoPagamentoEsperado,
                "O IdEventoPagamento retornado "
                        + "é diferente do esperado."
        );

        Optional<StatusEventoPagamento> statusMapeado =
                StatusEventoPagamento
                        .tentarObterPorCodigo(
                                pagamento.getStatus()
                        );

        if (statusMapeado.isEmpty()) {
            Assert.fail(
                    "O pagamento possui um status ainda "
                            + "não mapeado. "
                            + "IdEventoPagamento: "
                            + idEventoPagamentoEsperado
                            + ". Status encontrado: "
                            + pagamento.getStatus()
                            + " - "
                            + pagamento.getDescricaoStatus()
                            + "."
            );
        }

        StatusEventoPagamento status =
                statusMapeado.get();

        Assert.assertEquals(
                status,
                StatusEventoPagamento.PROCESSADO,
                "O pagamento ainda não foi processado. "
                        + "IdEventoPagamento: "
                        + idEventoPagamentoEsperado
                        + ". Status encontrado: "
                        + status.getCodigo()
                        + " - "
                        + status.getDescricao()
                        + "."
        );
    }

    private void validarParametros(
            Optional<PagamentoExistente> pagamentoEncontrado,
            long idEventoPagamentoEsperado
    ) {

        if (pagamentoEncontrado == null) {
            throw new IllegalArgumentException(
                    "O resultado da consulta não pode ser nulo. "
                            + "Utilize Optional.empty() quando "
                            + "o pagamento não for encontrado."
            );
        }

        if (idEventoPagamentoEsperado <= 0) {
            throw new IllegalArgumentException(
                    "O IdEventoPagamento esperado deve ser "
                            + "maior que zero."
            );
        }
    }

    private void anexarPagamentoNaoEncontrado(
            long idEventoPagamento
    ) {

        String evidencia =
                "IdEventoPagamento esperado: "
                        + idEventoPagamento
                        + System.lineSeparator()
                        + "Resultado: NÃO ENCONTRADO";

        Allure.addAttachment(
                "Validação do processamento do pagamento",
                "text/plain",
                evidencia
        );
    }

    private void anexarPagamentoEncontrado(
            PagamentoExistente pagamento
    ) {

        String evidencia =
                "IdEventoPagamento esperado: "
                        + pagamento
                                .getIdEventoPagamento()
                        + System.lineSeparator()
                        + "Status encontrado: "
                        + pagamento.getStatus()
                        + System.lineSeparator()
                        + "Descrição: "
                        + pagamento
                                .getDescricaoStatus();

        Allure.addAttachment(
                "Validação do processamento do pagamento",
                "text/plain",
                evidencia
        );
    }
}