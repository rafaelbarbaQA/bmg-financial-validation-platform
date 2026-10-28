package com.automation.features.pagamento.repository;

import com.automation.core.database.DatabaseAccessor;
import com.automation.features.pagamento.model.BoletoParaPagamento;
import com.automation.features.pagamento.model.TipoBoleto;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BoletoParaPagamentoRepository {

    private static final String CONSULTAR_BOLETO =
            "SELECT "
                    + "Id_Boleto AS ID_BOLETO, "
                    + "Id_TipoBoleto AS ID_TIPO_BOLETO, "
                    + "Id_Conta AS ID_CONTA, "
                    + "ValorBoleto AS VALOR_BOLETO, "
                    + "CONVERT(varchar(30), NossoNumero) "
                    + "AS NOSSO_NUMERO "
                    + "FROM BoletosEmitidos "
                    + "WHERE Id_Boleto = ?";

    private final DatabaseAccessor databaseAccessor;

    public BoletoParaPagamentoRepository() {

        this(
                new DatabaseAccessor()
        );
    }

    public BoletoParaPagamentoRepository(
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

    @Step("Consultar boleto para pagamento: {idBoleto}")
    public Optional<BoletoParaPagamento> buscarPorIdBoleto(
            long idBoleto
    ) throws SQLException {

        validarIdBoleto(idBoleto);

        anexarConsulta(idBoleto);

        List<Map<String, Object>> registros =
                databaseAccessor.executeQueryForList(
                        CONSULTAR_BOLETO,
                        idBoleto
                );

        if (registros.isEmpty()) {

            Allure.addAttachment(
                    "Resultado da consulta do boleto",
                    "text/plain",
                    "Nenhum boleto encontrado para o IdBoleto "
                            + idBoleto
                            + "."
            );

            return Optional.empty();
        }

        if (registros.size() > 1) {
            throw new IllegalStateException(
                    "Foram encontrados "
                            + registros.size()
                            + " boletos para o IdBoleto "
                            + idBoleto
                            + ". Era esperado exatamente um registro."
            );
        }

        Map<String, Object> registro =
                registros.get(0);

        Allure.addAttachment(
                "Resultado da consulta do boleto",
                "text/plain",
                registro.toString()
        );

        return Optional.of(
                mapearBoleto(registro)
        );
    }

    private BoletoParaPagamento mapearBoleto(
            Map<String, Object> registro
    ) {

        long idBoleto =
                obterLong(
                        registro,
                        "ID_BOLETO"
                );

        int codigoTipoBoleto =
                obterInt(
                        registro,
                        "ID_TIPO_BOLETO"
                );

        long idConta =
                obterLong(
                        registro,
                        "ID_CONTA"
                );

        BigDecimal valorBoleto =
                obterBigDecimal(
                        registro,
                        "VALOR_BOLETO"
                );

        String nossoNumero =
                obterString(
                        registro,
                        "NOSSO_NUMERO"
                );

        TipoBoleto tipoBoleto =
                TipoBoleto.obterPorCodigo(
                        codigoTipoBoleto
                );

        return new BoletoParaPagamento(
                idBoleto,
                tipoBoleto,
                idConta,
                valorBoleto,
                nossoNumero
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
                    valor.toString()
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

    private String obterString(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        registro,
                        coluna
                );

        return valor.toString().trim();
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

    private void validarIdBoleto(
            long idBoleto
    ) {

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "O IdBoleto deve ser maior que zero."
            );
        }
    }

    private void anexarConsulta(
            long idBoleto
    ) {

        Allure.addAttachment(
                "Consulta SQL - boleto para pagamento",
                "text/sql",
                CONSULTAR_BOLETO
        );

        Allure.addAttachment(
                "Parâmetro da consulta",
                "text/plain",
                "IdBoleto: " + idBoleto
        );
    }
}