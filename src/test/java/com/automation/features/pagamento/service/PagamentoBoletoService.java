package com.automation.features.pagamento.service;

import com.automation.features.pagamento.model.PagamentoBoleto;
import com.automation.features.pagamento.model.PagamentoExistente;

import com.automation.features.pagamento.repository.EventoExternoPagamentoRepository;
import com.automation.features.pagamento.repository.PagamentoDuplicidadeRepository;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.sql.SQLException;
import java.util.Optional;

public final class PagamentoBoletoService {

    private final PagamentoDuplicidadeRepository
            duplicidadeRepository;

    private final EventoExternoPagamentoRepository
            eventoRepository;

    public PagamentoBoletoService() {

        this(
                new PagamentoDuplicidadeRepository(),
                new EventoExternoPagamentoRepository()
        );
    }

    public PagamentoBoletoService(
            PagamentoDuplicidadeRepository duplicidadeRepository,
            EventoExternoPagamentoRepository eventoRepository
    ) {

        if (duplicidadeRepository == null) {
            throw new IllegalArgumentException(
                    "O PagamentoDuplicidadeRepository "
                            + "não pode ser nulo."
            );
        }

        if (eventoRepository == null) {
            throw new IllegalArgumentException(
                    "O EventoExternoPagamentoRepository "
                            + "não pode ser nulo."
            );
        }

        this.duplicidadeRepository =
                duplicidadeRepository;

        this.eventoRepository =
                eventoRepository;
    }

    @Step("Inserir pagamento do boleto com proteção contra duplicidade")
    public long inserir(
            PagamentoBoleto pagamento
    ) throws SQLException {

        validarPagamento(pagamento);

        verificarDuplicidade(
                pagamento
        );

        long idEventoPagamento =
                eventoRepository.inserir(
                        pagamento
                );

        Allure.step(
                "Pagamento inserido com IdEventoPagamento: "
                        + idEventoPagamento
        );

        return idEventoPagamento;
    }

    @Step("Verificar se o pagamento já foi inserido")
    private void verificarDuplicidade(
            PagamentoBoleto pagamento
    ) throws SQLException {

        Optional<PagamentoExistente> resultado =
                duplicidadeRepository
                        .buscarPagamentoExistente(
                                pagamento
                        );

        if (resultado.isEmpty()) {

            Allure.step(
                    "Pagamento não duplicado: "
                            + "inserção autorizada."
            );

            return;
        }

        PagamentoExistente pagamentoExistente =
                resultado.get();

        if (!pagamentoExistente
                .deveBloquearDuplicidade()) {

            Allure.step(
                    "Pagamento existente não bloqueia "
                            + "uma nova inserção. "
                            + pagamentoExistente
            );

            return;
        }

        Allure.step(
                "Pagamento duplicado bloqueado: "
                        + pagamentoExistente
        );

        throw new IllegalStateException(
                "O pagamento não foi inserido porque já existe "
                        + "um registro correspondente em "
                        + "EventosExternosPagamentos. "
                        + "IdEventoPagamento existente: "
                        + pagamentoExistente
                                .getIdEventoPagamento()
                        + ". Status: "
                        + pagamentoExistente.getStatus()
                        + " - "
                        + pagamentoExistente
                                .getDescricaoStatus()
                        + "."
        );
    }

    private void validarPagamento(
            PagamentoBoleto pagamento
    ) {

        if (pagamento == null) {
            throw new IllegalArgumentException(
                    "O pagamento não pode ser nulo."
            );
        }

        if (pagamento.getBoleto() == null) {
            throw new IllegalArgumentException(
                    "O boleto do pagamento não pode ser nulo."
            );
        }
    }
}