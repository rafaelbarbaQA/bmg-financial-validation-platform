package com.automation.features.quitacao.repository;

import com.automation.core.database.DatabaseAccessor;

import com.automation.features.quitacao.model.BoletoEmitido;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class BoletoEmitidoRepository {

    private static final String CONSULTAR_BOLETO =
            "SELECT TOP (1) "
                    + "Id_Conta, "
                    + "Id_Boleto "
                    + "FROM BoletosEmitidos "
                    + "WHERE Id_Conta = ? "
                    + "AND Id_Boleto = ?";

    private final DatabaseAccessor databaseAccessor;

    public BoletoEmitidoRepository() {

        this(
                new DatabaseAccessor()
        );
    }

    public BoletoEmitidoRepository(
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

    @Step("Consultar boleto emitido na base")
    public Optional<BoletoEmitido> buscarPorIdContaEIdBoleto(
            long idConta,
            long idBoleto
    ) throws SQLException {

        validarParametros(
                idConta,
                idBoleto
        );

        anexarEvidenciasDaConsulta(
                idConta,
                idBoleto
        );

        Optional<Map<String, Object>> resultado =
                databaseAccessor.executeQuery(
                        CONSULTAR_BOLETO,
                        idConta,
                        idBoleto
                );

        if (resultado.isEmpty()) {

            Allure.addAttachment(
                    "Resultado da consulta",
                    "text/plain",
                    "Nenhum boleto foi localizado."
            );

            return Optional.empty();
        }

        Map<String, Object> linha =
                resultado.get();

        Allure.addAttachment(
                "Resultado da consulta",
                "text/plain",
                linha.toString()
        );

        return Optional.of(
                mapearBoleto(linha)
        );
    }

    private BoletoEmitido mapearBoleto(
            Map<String, Object> linha
    ) {

        long idConta =
                obterValorLong(
                        linha,
                        "ID_CONTA"
                );

        long idBoleto =
                obterValorLong(
                        linha,
                        "ID_BOLETO"
                );

        return new BoletoEmitido(
                idConta,
                idBoleto
        );
    }

    private long obterValorLong(
            Map<String, Object> linha,
            String coluna
    ) {

        Object valor =
                linha.get(coluna);

        if (valor == null) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não foi retornada pela consulta."
            );
        }

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

    private void validarParametros(
            long idConta,
            long idBoleto
    ) {

        if (idConta <= 0) {
            throw new IllegalArgumentException(
                    "O IdConta deve ser maior que zero."
            );
        }

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "O IdBoleto deve ser maior que zero."
            );
        }
    }

    private void anexarEvidenciasDaConsulta(
            long idConta,
            long idBoleto
    ) {

        Allure.addAttachment(
                "Consulta SQL - BoletoEmitido",
                "text/sql",
                CONSULTAR_BOLETO
        );

        Allure.addAttachment(
                "Parâmetros da consulta",
                "text/plain",
                "IdConta: "
                        + idConta
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + idBoleto
        );
    }
}
