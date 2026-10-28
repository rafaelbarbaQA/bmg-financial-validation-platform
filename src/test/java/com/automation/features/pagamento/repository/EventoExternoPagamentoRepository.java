package com.automation.features.pagamento.repository;

import com.automation.core.database.DatabaseAccessor;
import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.features.pagamento.model.EventoExternoPagamento;
import com.automation.features.pagamento.model.PagamentoBoleto;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.math.BigDecimal;

import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import java.util.Map;
import java.util.Optional;

public final class EventoExternoPagamentoRepository {

    private static final int ID_AUTORIZACAO =
            9999;

    private static final int ID_OPERACAO =
            99;

    private static final int STATUS_PENDENTE =
            0;

    private static final BigDecimal RESTITUICAO_SAQUE =
            BigDecimal.ZERO;

    private static final DateTimeFormatter
            FORMATO_DATA_VENCIMENTO_PADRAO =
            DateTimeFormatter
                    .ofPattern("dd/MM/uuuu")
                    .withResolverStyle(
                            ResolverStyle.STRICT
                    );

    private static final String INSERIR_PAGAMENTO =
            "SET NOCOUNT ON; "
                    + "DECLARE @EventosInseridos TABLE ("
                    + "Id_EventoPagamento int NOT NULL"
                    + "); "
                    + "INSERT INTO EventosExternosPagamentos ("
                    + "Banco, "
                    + "DataMovimento, "
                    + "DataPagamento, "
                    + "NossoNumero, "
                    + "ValorPago, "
                    + "NumeroDocumento, "
                    + "Cartao, "
                    + "Id_MovimentoPagamentoPendente, "
                    + "DefasagemCobranca, "
                    + "Id_Autorizacao, "
                    + "Id_Conta, "
                    + "Id_Operacao, "
                    + "DataEntradaPagamento, "
                    + "DataVencimentoPadrao, "
                    + "DataVencimentoReal, "
                    + "RestituicaoSaque, "
                    + "Status"
                    + ") "
                    + "OUTPUT "
                    + "INSERTED.Id_EventoPagamento "
                    + "INTO @EventosInseridos ("
                    + "Id_EventoPagamento"
                    + ") "
                    + "SELECT "
                    + "PROD.Banco, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "NULL, "
                    + "CARTAO.Cartao, "
                    + "NULL, "
                    + "NULL, "
                    + "?, "
                    + "EC.Id_Conta, "
                    + "?, "
                    + "?, "
                    + "CONVERT("
                    + "char(10), "
                    + "EC.ProximoVencimentoPadrao, "
                    + "103"
                    + "), "
                    + "EC.ProximoVencimentoReal, "
                    + "?, "
                    + "? "
                    + "FROM EstadosContas EC "
                    + "INNER JOIN Produtos PROD "
                    + "ON PROD.Id_Produto = EC.Id_Produto "
                    + "CROSS APPLY ("
                    + "SELECT TOP (1) "
                    + "C.Cartao "
                    + "FROM Cartoes C "
                    + "WHERE C.Id_Conta = EC.Id_Conta "
                    + "AND C.Portador = 1 "
                    + "ORDER BY C.SequencialCartao DESC"
                    + ") CARTAO "
                    + "WHERE EC.Id_Conta = ?; "
                    + "SELECT "
                    + "Id_EventoPagamento "
                    + "AS ID_EVENTO_PAGAMENTO "
                    + "FROM @EventosInseridos;";

    private static final String CONSULTAR_PAGAMENTO =
            "SELECT "
                    + "Id_EventoPagamento "
                    + "AS ID_EVENTO_PAGAMENTO, "
                    + "Banco AS BANCO, "
                    + "DataMovimento AS DATA_MOVIMENTO, "
                    + "DataPagamento AS DATA_PAGAMENTO, "
                    + "CONVERT(varchar(20), NossoNumero) "
                    + "AS NOSSO_NUMERO, "
                    + "ValorPago AS VALOR_PAGO, "
                    + "RTRIM(Cartao) AS CARTAO, "
                    + "Id_Autorizacao AS ID_AUTORIZACAO, "
                    + "Id_Conta AS ID_CONTA, "
                    + "Id_Operacao AS ID_OPERACAO, "
                    + "DataEntradaPagamento "
                    + "AS DATA_ENTRADA_PAGAMENTO, "
                    + "DataVencimentoPadrao "
                    + "AS DATA_VENCIMENTO_PADRAO, "
                    + "DataVencimentoReal "
                    + "AS DATA_VENCIMENTO_REAL, "
                    + "RestituicaoSaque "
                    + "AS RESTITUICAO_SAQUE, "
                    + "Status AS STATUS "
                    + "FROM EventosExternosPagamentos "
                    + "WHERE Id_EventoPagamento = ?";

    private final DatabaseAccessor databaseAccessor;

    public EventoExternoPagamentoRepository() {

        this(
                new DatabaseAccessor()
        );
    }

    public EventoExternoPagamentoRepository(
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

    @Step("Inserir evento externo de pagamento")
    public long inserir(
            PagamentoBoleto pagamento
    ) throws SQLException {

        if (pagamento == null) {
            throw new IllegalArgumentException(
                    "O pagamento não pode ser nulo."
            );
        }

        EffectfulExecutionGuard
                .exigirAutorizacao();

        anexarEvidenciasDaInsercao(
                pagamento
        );

        Timestamp dataPagamento =
                Timestamp.valueOf(
                        pagamento
                                .getDataPagamento()
                                .atStartOfDay()
                );

        BigDecimal nossoNumero =
                new BigDecimal(
                        pagamento.getNossoNumero()
                );

        Optional<Map<String, Object>> resultado =
                databaseAccessor
                        .executeCommandReturningSingleRow(
                                INSERIR_PAGAMENTO,
                                dataPagamento,
                                dataPagamento,
                                nossoNumero,
                                pagamento
                                        .getValorPagamento(),
                                ID_AUTORIZACAO,
                                ID_OPERACAO,
                                dataPagamento,
                                RESTITUICAO_SAQUE,
                                STATUS_PENDENTE,
                                pagamento.getIdConta()
                        );

        if (resultado.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhum evento de pagamento foi inserido. "
                            + "Verifique se a conta possui estado, "
                            + "produto e cartão principal."
            );
        }

        long idEventoPagamento =
                obterLong(
                        resultado.get(),
                        "ID_EVENTO_PAGAMENTO"
                );

        Allure.addAttachment(
                "Identificador do evento inserido",
                "text/plain",
                "IdEventoPagamento: "
                        + idEventoPagamento
        );

        return idEventoPagamento;
    }

    @Step("Consultar evento externo de pagamento: {idEventoPagamento}")
    public Optional<EventoExternoPagamento>
    buscarPorIdEventoPagamento(
            long idEventoPagamento
    ) throws SQLException {

        if (idEventoPagamento <= 0) {
            throw new IllegalArgumentException(
                    "O IdEventoPagamento deve ser "
                            + "maior que zero."
            );
        }

        Allure.addAttachment(
                "Consulta SQL - evento de pagamento",
                "text/sql",
                CONSULTAR_PAGAMENTO
        );

        Allure.addAttachment(
                "Parâmetro da consulta",
                "text/plain",
                "IdEventoPagamento: "
                        + idEventoPagamento
        );

        Optional<Map<String, Object>> resultado =
                databaseAccessor.executeQuery(
                        CONSULTAR_PAGAMENTO,
                        idEventoPagamento
                );

        if (resultado.isEmpty()) {

            Allure.addAttachment(
                    "Resultado da consulta do pagamento",
                    "text/plain",
                    "Nenhum evento de pagamento encontrado."
            );

            return Optional.empty();
        }

        Map<String, Object> registro =
                resultado.get();

        Allure.addAttachment(
                "Resultado da consulta do pagamento",
                "text/plain",
                registro.toString()
        );

        return Optional.of(
                mapearEvento(registro)
        );
    }

    private EventoExternoPagamento mapearEvento(
            Map<String, Object> registro
    ) {

        return new EventoExternoPagamento(
                obterLong(
                        registro,
                        "ID_EVENTO_PAGAMENTO"
                ),
                obterInt(
                        registro,
                        "BANCO"
                ),
                obterLocalDateObrigatoria(
                        registro,
                        "DATA_MOVIMENTO"
                ),
                obterLocalDateObrigatoria(
                        registro,
                        "DATA_PAGAMENTO"
                ),
                obterStringObrigatoria(
                        registro,
                        "NOSSO_NUMERO"
                ),
                obterBigDecimal(
                        registro,
                        "VALOR_PAGO"
                ),
                obterStringObrigatoria(
                        registro,
                        "CARTAO"
                ),
                obterInt(
                        registro,
                        "ID_AUTORIZACAO"
                ),
                obterLong(
                        registro,
                        "ID_CONTA"
                ),
                obterInt(
                        registro,
                        "ID_OPERACAO"
                ),
                obterLocalDateObrigatoria(
                        registro,
                        "DATA_ENTRADA_PAGAMENTO"
                ),
                obterDataVencimentoPadrao(
                        registro
                ),
                obterLocalDateOpcional(
                        registro,
                        "DATA_VENCIMENTO_REAL"
                ),
                obterBigDecimal(
                        registro,
                        "RESTITUICAO_SAQUE"
                ),
                obterInt(
                        registro,
                        "STATUS"
                )
        );
    }

    private long obterLong(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        registro,
                        coluna
                );

        if (valor instanceof Number) {
            return ((Number) valor).longValue();
        }

        try {
            return Long.parseLong(
                    valor.toString().trim()
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

    private int obterInt(
            Map<String, Object> registro,
            String coluna
    ) {

        long valor =
                obterLong(
                        registro,
                        coluna
                );

        if (valor < Integer.MIN_VALUE
                || valor > Integer.MAX_VALUE) {

            throw new IllegalStateException(
                    "O valor da coluna "
                            + coluna
                            + " excede o limite de um int: "
                            + valor
            );
        }

        return (int) valor;
    }

    private BigDecimal obterBigDecimal(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        registro,
                        coluna
                );

        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }

        try {
            return new BigDecimal(
                    valor.toString().trim()
            );

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O valor da coluna "
                            + coluna
                            + " não é monetário: "
                            + valor,
                    exception
            );
        }
    }

    private String obterStringObrigatoria(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        registro,
                        coluna
                );

        String texto =
                valor.toString().trim();

        if (texto.isBlank()) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " foi retornada vazia."
            );
        }

        return texto;
    }

    private LocalDate obterLocalDateObrigatoria(
            Map<String, Object> registro,
            String coluna
    ) {

        LocalDate data =
                obterLocalDateOpcional(
                        registro,
                        coluna
                );

        if (data == null) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " foi retornada nula."
            );
        }

        return data;
    }

    private LocalDate obterLocalDateOpcional(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                registro.get(coluna);

        if (valor == null) {
            return null;
        }

        if (valor instanceof Timestamp) {
            return ((Timestamp) valor)
                    .toLocalDateTime()
                    .toLocalDate();
        }

        if (valor instanceof java.sql.Date) {
            return ((java.sql.Date) valor)
                    .toLocalDate();
        }

        if (valor instanceof LocalDate) {
            return (LocalDate) valor;
        }

        if (valor instanceof LocalDateTime) {
            return ((LocalDateTime) valor)
                    .toLocalDate();
        }

        throw new IllegalStateException(
                "A coluna "
                        + coluna
                        + " não contém uma data reconhecida: "
                        + valor
        );
    }

    private LocalDate obterDataVencimentoPadrao(
            Map<String, Object> registro
    ) {

        Object valor =
                registro.get(
                        "DATA_VENCIMENTO_PADRAO"
                );

        if (valor == null
                || valor.toString().isBlank()) {

            return null;
        }

        String texto =
                valor.toString().trim();

        try {
            return LocalDate.parse(
                    texto,
                    FORMATO_DATA_VENCIMENTO_PADRAO
            );

        } catch (DateTimeParseException exception) {
            throw new IllegalStateException(
                    "A DataVencimentoPadrao possui "
                            + "formato inválido: "
                            + texto
                            + ". Esperado: dd/MM/yyyy.",
                    exception
            );
        }
    }

    private Object obterValorObrigatorio(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                registro.get(coluna);

        if (valor == null) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não foi retornada ou está nula."
            );
        }

        return valor;
    }

    private void anexarEvidenciasDaInsercao(
            PagamentoBoleto pagamento
    ) {

        Allure.addAttachment(
                "Comando SQL - inserir pagamento",
                "text/sql",
                INSERIR_PAGAMENTO
        );

        String parametros =
                "IdBoleto: "
                        + pagamento.getIdBoleto()
                        + System.lineSeparator()
                        + "IdConta: "
                        + pagamento.getIdConta()
                        + System.lineSeparator()
                        + "TipoBoleto: "
                        + pagamento.getTipoBoleto()
                        + System.lineSeparator()
                        + "Modalidade: "
                        + pagamento.getModalidade()
                        + System.lineSeparator()
                        + "NossoNumero: "
                        + pagamento.getNossoNumero()
                        + System.lineSeparator()
                        + "ValorPagamento: "
                        + pagamento.getValorPagamento()
                        + System.lineSeparator()
                        + "DataPagamento: "
                        + pagamento.getDataPagamento()
                        + System.lineSeparator()
                        + "IdAutorizacao: "
                        + ID_AUTORIZACAO
                        + System.lineSeparator()
                        + "IdOperacao: "
                        + ID_OPERACAO
                        + System.lineSeparator()
                        + "Status: "
                        + STATUS_PENDENTE;

        Allure.addAttachment(
                "Parâmetros da inserção do pagamento",
                "text/plain",
                parametros
        );
    }
}