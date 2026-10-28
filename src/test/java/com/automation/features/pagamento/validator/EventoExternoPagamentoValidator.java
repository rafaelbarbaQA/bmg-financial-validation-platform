package com.automation.features.pagamento.validator;

import com.automation.features.pagamento.model.BoletoParaPagamento;
import com.automation.features.pagamento.model.EventoExternoPagamento;
import com.automation.features.pagamento.model.PagamentoBoleto;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import org.testng.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class EventoExternoPagamentoValidator {

    private static final int ID_AUTORIZACAO_ESPERADO =
            9999;

    private static final int ID_OPERACAO_ESPERADO =
            99;

    private static final int STATUS_ESPERADO =
            0;

    private static final BigDecimal RESTITUICAO_SAQUE_ESPERADA =
            BigDecimal.ZERO;

    @Step("Validar pagamento inserido em EventosExternosPagamentos")
    public void validar(
            EventoExternoPagamento evento,
            PagamentoBoleto pagamento,
            long idEventoPagamentoEsperado
    ) {

        validarParametros(
                evento,
                pagamento,
                idEventoPagamentoEsperado
        );

        BoletoParaPagamento boleto =
                pagamento.getBoleto();

        anexarResumoDaValidacao(
                evento,
                pagamento
        );

        validarIdentificadorDoEvento(
                evento,
                idEventoPagamentoEsperado
        );

        validarConta(
                evento,
                boleto
        );

        validarNossoNumero(
                evento,
                boleto
        );

        validarValorPago(
                evento,
                pagamento
        );

        validarDatas(
                evento,
                pagamento.getDataPagamento()
        );

        validarDadosOperacionais(evento);

        validarDadosComplementares(evento);
    }

    @Step("Validar identificador do evento de pagamento")
    private void validarIdentificadorDoEvento(
            EventoExternoPagamento evento,
            long idEventoPagamentoEsperado
    ) {

        Assert.assertEquals(
                evento.getIdEventoPagamento(),
                idEventoPagamentoEsperado,
                "O IdEventoPagamento consultado é diferente "
                        + "do identificador retornado pela inserção."
        );

        Assert.assertTrue(
                evento.getIdEventoPagamento() > 0,
                "O IdEventoPagamento deve ser maior que zero."
        );
    }

    @Step("Validar conta do pagamento")
    private void validarConta(
            EventoExternoPagamento evento,
            BoletoParaPagamento boleto
    ) {

        Assert.assertEquals(
                evento.getIdConta(),
                boleto.getIdConta(),
                "O IdConta do pagamento é diferente "
                        + "do IdConta do boleto."
        );
    }

    @Step("Validar NossoNumero do pagamento")
    private void validarNossoNumero(
            EventoExternoPagamento evento,
            BoletoParaPagamento boleto
    ) {

        Assert.assertNotNull(
                evento.getNossoNumero(),
                "O NossoNumero não foi gravado no pagamento."
        );

        Assert.assertEquals(
                evento.getNossoNumero(),
                boleto.getNossoNumero(),
                "O NossoNumero gravado é diferente "
                        + "do NossoNumero do boleto."
        );
    }

    @Step("Validar valor pago")
    private void validarValorPago(
            EventoExternoPagamento evento,
            PagamentoBoleto pagamento
    ) {

        Assert.assertNotNull(
                evento.getValorPago(),
                "O ValorPago não foi gravado."
        );

        Assert.assertEquals(
                evento.getValorPago()
                        .compareTo(
                                pagamento.getValorPagamento()
                        ),
                0,
                "O ValorPago gravado é diferente "
                        + "do valor solicitado."
        );
    }

    @Step("Validar datas do pagamento")
    private void validarDatas(
            EventoExternoPagamento evento,
            LocalDate dataPagamentoEsperada
    ) {

        Assert.assertEquals(
                evento.getDataMovimento(),
                dataPagamentoEsperada,
                "A DataMovimento é diferente "
                        + "da data informada."
        );

        Assert.assertEquals(
                evento.getDataPagamento(),
                dataPagamentoEsperada,
                "A DataPagamento é diferente "
                        + "da data informada."
        );

        Assert.assertEquals(
                evento.getDataEntradaPagamento(),
                dataPagamentoEsperada,
                "A DataEntradaPagamento é diferente "
                        + "da data informada."
        );
    }

    @Step("Validar dados operacionais do pagamento")
    private void validarDadosOperacionais(
            EventoExternoPagamento evento
    ) {

        Assert.assertEquals(
                evento.getIdAutorizacao(),
                ID_AUTORIZACAO_ESPERADO,
                "O IdAutorizacao deve ser 9999."
        );

        Assert.assertEquals(
                evento.getIdOperacao(),
                ID_OPERACAO_ESPERADO,
                "O IdOperacao deve ser 99."
        );

        Assert.assertEquals(
                evento.getStatus(),
                STATUS_ESPERADO,
                "O status inicial do pagamento deve ser zero."
        );

        Assert.assertNotNull(
                evento.getRestituicaoSaque(),
                "A RestituicaoSaque não foi retornada."
        );

        Assert.assertEquals(
                evento.getRestituicaoSaque()
                        .compareTo(
                                RESTITUICAO_SAQUE_ESPERADA
                        ),
                0,
                "A RestituicaoSaque deve ser zero."
        );
    }

    @Step("Validar dados complementares do pagamento")
    private void validarDadosComplementares(
            EventoExternoPagamento evento
    ) {

        Assert.assertTrue(
                evento.getBanco() > 0,
                "O código do banco não foi preenchido corretamente."
        );

        Assert.assertNotNull(
                evento.getCartao(),
                "O cartão principal não foi gravado."
        );

        Assert.assertFalse(
                evento.getCartao().isBlank(),
                "O cartão principal foi gravado vazio."
        );

        Assert.assertNotNull(
                evento.getDataVencimentoPadrao(),
                "A DataVencimentoPadrao não foi preenchida."
        );

        Assert.assertNotNull(
                evento.getDataVencimentoReal(),
                "A DataVencimentoReal não foi preenchida."
        );
    }

    private void validarParametros(
            EventoExternoPagamento evento,
            PagamentoBoleto pagamento,
            long idEventoPagamentoEsperado
    ) {

        if (evento == null) {
            throw new IllegalArgumentException(
                    "O evento externo de pagamento "
                            + "não pode ser nulo."
            );
        }

        if (pagamento == null) {
            throw new IllegalArgumentException(
                    "O pagamento esperado não pode ser nulo."
            );
        }

        if (idEventoPagamentoEsperado <= 0) {
            throw new IllegalArgumentException(
                    "O IdEventoPagamento esperado "
                            + "deve ser maior que zero."
            );
        }
    }

    private void anexarResumoDaValidacao(
            EventoExternoPagamento evento,
            PagamentoBoleto pagamento
    ) {

        BoletoParaPagamento boleto =
                pagamento.getBoleto();

        String resumo =
                "VALIDAÇÃO DO PAGAMENTO"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "IdEventoPagamento: "
                        + evento.getIdEventoPagamento()
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + boleto.getIdBoleto()
                        + System.lineSeparator()
                        + "TipoBoleto: "
                        + boleto.getTipoBoleto()
                        + System.lineSeparator()
                        + "IdConta: "
                        + evento.getIdConta()
                        + System.lineSeparator()
                        + "NossoNumero: "
                        + evento.getNossoNumero()
                        + System.lineSeparator()
                        + "ValorPago: "
                        + evento.getValorPago()
                        + System.lineSeparator()
                        + "DataPagamento: "
                        + evento.getDataPagamento()
                        + System.lineSeparator()
                        + "IdAutorizacao: "
                        + evento.getIdAutorizacao()
                        + System.lineSeparator()
                        + "IdOperacao: "
                        + evento.getIdOperacao()
                        + System.lineSeparator()
                        + "Status: "
                        + evento.getStatus();

        Allure.addAttachment(
                "Resumo da validação do pagamento",
                "text/plain",
                resumo
        );
    }
}