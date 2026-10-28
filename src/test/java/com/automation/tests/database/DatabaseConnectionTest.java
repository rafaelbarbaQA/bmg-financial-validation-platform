package com.automation.tests.database;

import com.automation.core.config.ConfigReader;
import com.automation.core.database.DatabaseAccessor;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

@Epic("Infraestrutura de Automação")
@Feature("Banco de Dados")
@Story("Conexão com SQL Server")
@Owner("QA Automation")
@Severity(SeverityLevel.BLOCKER)

public class DatabaseConnectionTest {

    private static final String CONSULTA_CONEXAO =
            "SELECT "
                    + "DB_NAME() AS DATABASE_NAME, "
                    + "1 AS CONNECTION_OK";

    @Test(groups = {"database", "smoke"})

@Description(
        "Valida que o framework consegue conectar ao SQL Server "
                + "e acessar a base configurada."
)
public void deveConectarAoBancoConfiguradoComSucesso()
        throws SQLException {

    Map<String, Object> resultado =
            consultarBanco();

    validarConexao(resultado);
    validarNomeDaBase(resultado);
}

    @Step("Executar consulta de validação da conexão")
    private Map<String, Object> consultarBanco()
            throws SQLException {

        DatabaseAccessor databaseAccessor =
                new DatabaseAccessor();

        Allure.addAttachment(
                "Consulta SQL",
                "text/sql",
                CONSULTA_CONEXAO
        );

        Optional<Map<String, Object>> resultado =
                databaseAccessor.executeQuery(
                        CONSULTA_CONEXAO
                );

        Assert.assertTrue(
                resultado.isPresent(),
                "A consulta de conexão não retornou resultado."
        );

        Map<String, Object> linha =
                resultado.get();

        Allure.addAttachment(
                "Resultado da consulta",
                "text/plain",
                linha.toString()
        );

        return linha;
    }

    @Step("Validar conexão estabelecida com sucesso")
    private void validarConexao(
            Map<String, Object> resultado
    ) {

        Object valorConexao =
                resultado.get("CONNECTION_OK");

        Assert.assertNotNull(
                valorConexao,
                "A coluna CONNECTION_OK não foi retornada."
        );

        Assert.assertTrue(
                valorConexao instanceof Number,
                "O valor de CONNECTION_OK não é numérico."
        );

        Assert.assertEquals(
                ((Number) valorConexao).intValue(),
                1,
                "A conexão com o banco não foi validada."
        );
    }

    @Step("Validar nome da base acessada")
private void validarNomeDaBase(
        Map<String, Object> resultado
) {

    Object nomeBaseRetornado =
            resultado.get("DATABASE_NAME");

    Assert.assertNotNull(
            nomeBaseRetornado,
            "O nome da base não foi retornado."
    );

    String nomeBaseEsperado =
            ConfigReader.getProperty("DB_NAME");

    if (nomeBaseEsperado == null
            || nomeBaseEsperado.isBlank()) {

        nomeBaseEsperado = "bmg";
    }

    String baseRetornada =
            nomeBaseRetornado
                    .toString()
                    .trim();

    String baseEsperada =
            nomeBaseEsperado.trim();

    Assert.assertTrue(
            baseRetornada.equalsIgnoreCase(
                    baseEsperada
            ),
            "A conexão foi realizada em uma base diferente. "
                    + "Esperada: "
                    + baseEsperada
                    + " | Retornada: "
                    + baseRetornada
    );
}
}
