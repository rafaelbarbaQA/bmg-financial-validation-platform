package com.automation.core.database;

import com.automation.core.config.ConfigReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

public class DatabaseAccessor {

    @FunctionalInterface
    public interface TransactionCallback<T> {

        T execute(
                Connection connection
        ) throws Exception;
    }

    private static final int QUERY_TIMEOUT_SECONDS =
            30;

    private final String dbUrl;
    private final String user;
    private final String password;

    public DatabaseAccessor() {

        String dbIp =
                obterConfiguracaoObrigatoria(
                        "DB_IP"
                );

        String dbPort =
                obterConfiguracaoOpcional(
                        "DB_PORT",
                        "1433"
                );

        String dbName =
                obterConfiguracaoOpcional(
                        "DB_NAME",
                        "bmg"
                );

        String encrypt =
                obterConfiguracaoOpcional(
                        "DB_ENCRYPT",
                        "true"
                );

        String trustServerCertificate =
                obterConfiguracaoOpcional(
                        "DB_TRUST_SERVER_CERTIFICATE",
                        "true"
                );

        this.user =
                obterVariavelAmbienteObrigatoria(
                        "DB_USER"
                );

        this.password =
                obterVariavelAmbienteObrigatoria(
                        "DB_PASSWORD"
                );

        this.dbUrl =
                "jdbc:sqlserver://"
                        + dbIp
                        + ":"
                        + dbPort
                        + ";databaseName="
                        + dbName
                        + ";encrypt="
                        + encrypt
                        + ";trustServerCertificate="
                        + trustServerCertificate;
    }

    /**
     * Executa uma consulta que deve retornar
     * somente o primeiro registro.
     */
    public <T> T inTransaction(
            TransactionCallback<T> callback
    ) {
        Objects.requireNonNull(
                callback,
                "Transaction callback cannot be null."
        );

        try (
                Connection connection =
                        abrirConexao()
        ) {
            connection.setAutoCommit(false);

            try {
                T result =
                        callback.execute(
                                connection
                        );

                connection.commit();

                return result;

            } catch (Exception exception) {

                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {
                    exception.addSuppressed(
                            rollbackException
                    );
                }

                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                }

                throw new IllegalStateException(
                        "Transactional database operation failed.",
                        exception
                );
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not execute database transaction.",
                    exception
            );
        }
    }

    public Optional<Map<String, Object>> executeQuery(
            String sqlQuery,
            Object... parameters
    ) throws SQLException {

        validarQuery(sqlQuery);

        try (
                Connection connection =
                        abrirConexao();

                PreparedStatement statement =
                        prepararStatement(
                                connection,
                                sqlQuery,
                                parameters
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            if (!resultSet.next()) {
                return Optional.empty();
            }

            return Optional.of(
                    mapearLinha(resultSet)
            );
        }
    }

    /**
     * Executa uma consulta que pode retornar
     * vários registros.
     */
    public List<Map<String, Object>> executeQueryForList(
            String sqlQuery,
            Object... parameters
    ) throws SQLException {

        validarQuery(sqlQuery);

        List<Map<String, Object>> rows =
                new ArrayList<>();

        try (
                Connection connection =
                        abrirConexao();

                PreparedStatement statement =
                        prepararStatement(
                                connection,
                                sqlQuery,
                                parameters
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                rows.add(
                        mapearLinha(resultSet)
                );
            }
        }

        return rows;
    }

    /**
     * Executa um comando que deve retornar
     * no máximo um registro.
     *
     * Esse método é utilizado em comandos como:
     *
     * INSERT ... OUTPUT INSERTED.Coluna
     *
     * A transação somente será confirmada quando
     * exatamente um registro for retornado.
     */
    public Optional<Map<String, Object>>
    executeCommandReturningSingleRow(
            String sqlCommand,
            Object... parameters
    ) throws SQLException {

        validarQuery(sqlCommand);

        try (
                Connection connection =
                        abrirConexao()
        ) {

            connection.setAutoCommit(false);

            try (
                    PreparedStatement statement =
                            prepararStatement(
                                    connection,
                                    sqlCommand,
                                    parameters
                            );

                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {

                    connection.rollback();

                    return Optional.empty();
                }

                Map<String, Object> row =
                        mapearLinha(resultSet);

                if (resultSet.next()) {
                    throw new IllegalStateException(
                            "O comando SQL retornou mais de "
                                    + "um registro. A operação foi "
                                    + "cancelada para evitar uma "
                                    + "alteração em massa."
                    );
                }

                connection.commit();

                return Optional.of(row);

            } catch (
                    SQLException
                            | RuntimeException exception
            ) {

                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {

                    exception.addSuppressed(
                            rollbackException
                    );
                }

                throw exception;
            }
        }
    }

    /**
     * Executa um comando SQL transacional que nao depende
     * do retorno de registros.
     *
     * Pode ser utilizado para stored procedures, UPDATE,
     * DELETE e outros comandos com efeito.
     */
    public void executeCommand(
            String sqlCommand,
            Object... parameters
    ) throws SQLException {

        validarQuery(sqlCommand);

        try (
                Connection connection =
                        abrirConexao()
        ) {
            connection.setAutoCommit(false);

            try (
                    PreparedStatement statement =
                            prepararStatement(
                                    connection,
                                    sqlCommand,
                                    parameters
                            )
            ) {
                statement.execute();

                connection.commit();

            } catch (
                    SQLException
                            | RuntimeException exception
            ) {
                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {
                    exception.addSuppressed(
                            rollbackException
                    );
                }

                throw exception;
            }
        }
    }
    private Connection abrirConexao()
            throws SQLException {

        return DriverManager.getConnection(
                dbUrl,
                user,
                password
        );
    }

    private PreparedStatement prepararStatement(
            Connection connection,
            String sqlQuery,
            Object... parameters
    ) throws SQLException {

        PreparedStatement statement =
                connection.prepareStatement(
                        sqlQuery
                );

        statement.setQueryTimeout(
                QUERY_TIMEOUT_SECONDS
        );

        if (parameters == null) {
            return statement;
        }

        for (
                int index = 0;
                index < parameters.length;
                index++
        ) {

            statement.setObject(
                    index + 1,
                    parameters[index]
            );
        }

        return statement;
    }

    private Map<String, Object> mapearLinha(
            ResultSet resultSet
    ) throws SQLException {

        ResultSetMetaData metadata =
                resultSet.getMetaData();

        int columnCount =
                metadata.getColumnCount();

        Map<String, Object> row =
                new LinkedHashMap<>();

        for (
                int index = 1;
                index <= columnCount;
                index++
        ) {

            String columnName =
                    metadata
                            .getColumnLabel(index)
                            .toUpperCase(Locale.ROOT);

            Object value =
                    resultSet.getObject(index);

            row.put(
                    columnName,
                    value
            );
        }

        return row;
    }

    private void validarQuery(
            String sqlQuery
    ) {

        if (sqlQuery == null
                || sqlQuery.isBlank()) {

            throw new IllegalArgumentException(
                    "A consulta SQL não pode ser "
                            + "nula ou vazia."
            );
        }
    }

    private String obterConfiguracaoObrigatoria(
            String propriedade
    ) {

        String valor =
                ConfigReader.getProperty(
                        propriedade
                );

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalStateException(
                    "A propriedade "
                            + propriedade
                            + " não foi encontrada "
                            + "na configuração."
            );
        }

        return valor.trim();
    }

    private String obterConfiguracaoOpcional(
            String propriedade,
            String valorPadrao
    ) {

        String valor =
                ConfigReader.getProperty(
                        propriedade
                );

        if (valor == null
                || valor.isBlank()) {

            return valorPadrao;
        }

        return valor.trim();
    }

    private String obterVariavelAmbienteObrigatoria(
            String nomeVariavel
    ) {

        String valor =
                System.getenv(
                        nomeVariavel
                );

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalStateException(
                    "A variável de ambiente "
                            + nomeVariavel
                            + " não foi configurada."
            );
        }

        return valor;
    }
}