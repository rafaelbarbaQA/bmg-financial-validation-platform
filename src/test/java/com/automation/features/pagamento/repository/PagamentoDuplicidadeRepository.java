package com.automation.features.pagamento.repository;

import com.automation.core.database.DatabaseAccessor;

import com.automation.features.pagamento.model.BoletoParaPagamento;
import com.automation.features.pagamento.model.PagamentoBoleto;
import com.automation.features.pagamento.model.PagamentoExistente;
import com.automation.features.pagamento.model.TipoBoleto;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public final class PagamentoDuplicidadeRepository {

    private static final int ID_AUTORIZACAO_AUTOMACAO =
            9999;

    private static final int ID_OPERACAO_AUTOMACAO =
            99;

    private static final String CONSULTAR_DUPLICIDADE_QUITACAO =
            "SELECT TOP (1) "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO, "
                    + "Status AS STATUS_PAGAMENTO "
                    + "FROM EventosExternosPagamentos "
                    + "WHERE Id_Conta = ? "
                    + "AND NossoNumero = ? "
                    + "AND Id_Autorizacao = ? "
                    + "AND Id_Operacao = ? "
                    + "ORDER BY Id_EventoPagamento DESC";

    private static final String CONSULTAR_DUPLICIDADE_FATURA =
            "SELECT TOP (1) "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO, "
                    + "Status AS STATUS_PAGAMENTO "
                    + "FROM EventosExternosPagamentos "
                    + "WHERE Id_Conta = ? "
                    + "AND NossoNumero = ? "
                    + "AND Id_Autorizacao = ? "
                    + "AND Id_Operacao = ? "
                    + "AND CONVERT(date, DataPagamento) = ? "
                    + "AND ValorPago = ? "
                    + "ORDER BY Id_EventoPagamento DESC";

    private final DatabaseAccessor databaseAccessor;

    public PagamentoDuplicidadeRepository() {

        this(
                new DatabaseAccessor()
        );
    }

    public PagamentoDuplicidadeRepository(
            DatabaseAccessor databaseAccessor
    ) {

        if (databaseAccessor == null) {
            throw new IllegalArgumentException(
                    "O DatabaseAccessor não pode ser nulo."
            );
        }

        this.databaseAccessor =
                databaseAccessor;
    }

    @Step("Consultar possível pagamento duplicado")
    public Optional<PagamentoExistente>
    buscarPagamentoExistente(
            PagamentoBoleto pagamento
    ) throws SQLException {

        validarPagamento(pagamento);

        BoletoParaPagamento boleto =
                pagamento.getBoleto();

        Optional<Map<String, Object>> resultado;

        if (boleto.getTipoBoleto()
                == TipoBoleto.QUITACAO) {

            resultado =
                    consultarDuplicidadeQuitacao(
                            boleto
                    );

        } else if (boleto.getTipoBoleto()
                == TipoBoleto.FATURA_MENSAL) {

            resultado =
                    consultarDuplicidadeFatura(
                            pagamento
                    );

        } else {
            throw new IllegalStateException(
                    "Tipo de boleto não suportado: "
                            + boleto.getTipoBoleto()
            );
        }

        if (resultado.isEmpty()) {

            Allure.addAttachment(
                    "Resultado da verificação de duplicidade",
                    "text/plain",
                    "Nenhum pagamento duplicado foi localizado."
            );

            return Optional.empty();
        }

        PagamentoExistente pagamentoExistente =
                mapearPagamentoExistente(
                        resultado.get()
                );

        Allure.addAttachment(
                "Resultado da verificação de duplicidade",
                "text/plain",
                pagamentoExistente.toString()
        );

        return Optional.of(
                pagamentoExistente
        );
    }

    private Optional<Map<String, Object>>
    consultarDuplicidadeQuitacao(
            BoletoParaPagamento boleto
    ) throws SQLException {

        anexarConsulta(
                CONSULTAR_DUPLICIDADE_QUITACAO,
                boleto,
                null
        );

        return databaseAccessor.executeQuery(
                CONSULTAR_DUPLICIDADE_QUITACAO,
                boleto.getIdConta(),
                converterNossoNumero(
                        boleto.getNossoNumero()
                ),
                ID_AUTORIZACAO_AUTOMACAO,
                ID_OPERACAO_AUTOMACAO
        );
    }

    private Optional<Map<String, Object>>
    consultarDuplicidadeFatura(
            PagamentoBoleto pagamento
    ) throws SQLException {

        BoletoParaPagamento boleto =
                pagamento.getBoleto();

        anexarConsulta(
                CONSULTAR_DUPLICIDADE_FATURA,
                boleto,
                pagamento
        );

        return databaseAccessor.executeQuery(
                CONSULTAR_DUPLICIDADE_FATURA,
                boleto.getIdConta(),
                converterNossoNumero(
                        boleto.getNossoNumero()
                ),
                ID_AUTORIZACAO_AUTOMACAO,
                ID_OPERACAO_AUTOMACAO,
                Date.valueOf(
                        pagamento.getDataPagamento()
                ),
                pagamento.getValorPagamento()
        );
    }

    private PagamentoExistente mapearPagamentoExistente(
            Map<String, Object> linha
    ) {

        long idEventoPagamento =
                obterValorLong(
                        linha,
                        "ID_EVENTO_PAGAMENTO"
                );

        int status =
                obterValorInt(
                        linha,
                        "STATUS_PAGAMENTO"
                );

        return new PagamentoExistente(
                idEventoPagamento,
                status
        );
    }

    private BigDecimal converterNossoNumero(
            String nossoNumero
    ) {

        try {
            return new BigDecimal(
                    nossoNumero
            );

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O NossoNumero não contém "
                            + "um valor numérico válido: "
                            + nossoNumero,
                    exception
            );
        }
    }

    private long obterValorLong(
            Map<String, Object> linha,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        linha,
                        coluna
                );

        if (valor instanceof Number) {
            return ((Number) valor)
                    .longValue();
        }

        try {
            return Long.parseLong(
                    valor.toString()
            );

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O valor da coluna "
                            + coluna
                            + " não é numérico: "
                            + valor,
                    exception
            );
        }
    }

    private int obterValorInt(
            Map<String, Object> linha,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        linha,
                        coluna
                );

        if (valor instanceof Number) {
            return ((Number) valor)
                    .intValue();
        }

        try {
            return Integer.parseInt(
                    valor.toString()
            );

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O valor da coluna "
                            + coluna
                            + " não é numérico: "
                            + valor,
                    exception
            );
        }
    }

    private Object obterValorObrigatorio(
            Map<String, Object> linha,
            String coluna
    ) {

        Object valor =
                linha.get(coluna);

        if (valor == null) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não foi retornada "
                            + "ou possui valor nulo."
            );
        }

        return valor;
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

    private void anexarConsulta(
            String consulta,
            BoletoParaPagamento boleto,
            PagamentoBoleto pagamento
    ) {

        Allure.addAttachment(
                "Consulta SQL - Duplicidade de pagamento",
                "text/sql",
                consulta
        );

        StringBuilder parametros =
                new StringBuilder()
                        .append("IdConta: ")
                        .append(boleto.getIdConta())
                        .append(System.lineSeparator())
                        .append("NossoNumero: ")
                        .append(boleto.getNossoNumero())
                        .append(System.lineSeparator())
                        .append("TipoBoleto: ")
                        .append(boleto.getTipoBoleto())
                        .append(System.lineSeparator())
                        .append("IdAutorizacao: ")
                        .append(ID_AUTORIZACAO_AUTOMACAO)
                        .append(System.lineSeparator())
                        .append("IdOperacao: ")
                        .append(ID_OPERACAO_AUTOMACAO);

        if (pagamento != null) {
            parametros
                    .append(System.lineSeparator())
                    .append("DataPagamento: ")
                    .append(
                            pagamento.getDataPagamento()
                    )
                    .append(System.lineSeparator())
                    .append("ValorPagamento: ")
                    .append(
                            pagamento.getValorPagamento()
                    );
        }

        Allure.addAttachment(
                "Parâmetros da verificação de duplicidade",
                "text/plain",
                parametros.toString()
        );
    }
}