package com.automation.tools.validation;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CadocValorSaidaMassValidationIT {

    private static final String CALCULATE_COMPARATIVE_VALUE =
            "WITH Lucro AS ( "
                    + "    SELECT TC.Id_Conta, SUM(TC.Valor) AS Valor "
                    + "    FROM CONTASCESSAO T "
                    + "    INNER JOIN TransacoesCorrentes TC WITH (NOLOCK) "
                    + "        ON T.Id_Conta = TC.Id_Conta "
                    + "    WHERE TC.DataGeracao = ( "
                    + "        SELECT Data "
                    + "        FROM ParametrosEmissores "
                    + "        WHERE Codigo = 'DataTransacaoCessao' "
                    + "    ) "
                    + "      AND TC.Id_TipoTransacao = 14015 "
                    + "      AND TC.Id_Conta = ? "
                    + "    GROUP BY TC.Id_Conta "
                    + "), "
                    + "Prejuizo AS ( "
                    + "    SELECT TC.Id_Conta, SUM(TC.Valor) AS Valor "
                    + "    FROM CONTASCESSAO T "
                    + "    INNER JOIN TransacoesCorrentes TC WITH (NOLOCK) "
                    + "        ON T.Id_Conta = TC.Id_Conta "
                    + "    WHERE TC.DataGeracao = ( "
                    + "        SELECT Data "
                    + "        FROM ParametrosEmissores "
                    + "        WHERE Codigo = 'DataTransacaoCessao' "
                    + "    ) "
                    + "      AND TC.Id_TipoTransacao = 14017 "
                    + "      AND TC.Id_Conta = ? "
                    + "    GROUP BY TC.Id_Conta "
                    + ") "
                    + "SELECT "
                    + "    (CC.SaldoParcelaCompra - CC.SaldoJurosCompra) "
                    + "  + (CC.SaldoParcelaSaque - CC.SaldoJurosSaque) "
                    + "  + ISNULL(L.Valor, 0) "
                    + "  - ISNULL(P.Valor, 0) AS VALOR_COMPARATIVO "
                    + "FROM CONTASCESSAO CC "
                    + "LEFT JOIN Lucro L "
                    + "    ON CC.Id_Conta = L.Id_Conta "
                    + "LEFT JOIN Prejuizo P "
                    + "    ON CC.Id_Conta = P.Id_Conta "
                    + "WHERE CC.Id_Conta = ?";

    private static final String FIND_VALOR_SAIDA =
            "SELECT ValorSaida "
                    + "FROM CADOCBaixasEmissor "
                    + "WHERE Id_Conta = ? "
                    + "AND ValorSaida > 0";

    @Test
    public void shouldValidateValorComparativoAgainstValorSaida() {

        Set<Long> accounts =
                loadAccounts();

        String databaseUrl =
                buildDatabaseUrl();

        String databaseUser =
                requireCredential(
                        "db.user",
                        "DB_USER"
                );

        String databasePassword =
                requireCredential(
                        "db.password",
                        "DB_PASSWORD"
                );

        int passed = 0;
        int failed = 0;
        int errors = 0;

        System.out.println();
        System.out.println(
                "======================================================================"
        );
        System.out.println(
                " CADOC - VALIDACAO VALOR COMPARATIVO x VALOR SAIDA"
        );
        System.out.println(
                "======================================================================"
        );

        for (Long accountId : accounts) {

            AccountValidationResult result =
                    validateAccount(
                            databaseUrl,
                            databaseUser,
                            databasePassword,
                            accountId
                    );

            printResult(result);

            if (result.error != null) {
                errors++;
            } else if (result.passed) {
                passed++;
            } else {
                failed++;
            }
        }

        System.out.println();
        System.out.println(
                "======================================================================"
        );
        System.out.println(
                " TOTAL.................: " + accounts.size()
        );
        System.out.println(
                " PASS..................: " + passed
        );
        System.out.println(
                " FAIL..................: " + failed
        );
        System.out.println(
                " ERROR.................: " + errors
        );
        System.out.println(
                "======================================================================"
        );

        if (failed > 0 || errors > 0) {

            Assert.fail(
                    "CADOC validation finished with "
                            + failed
                            + " failure(s) and "
                            + errors
                            + " error(s)."
            );
        }
    }

    private static AccountValidationResult validateAccount(
            String databaseUrl,
            String databaseUser,
            String databasePassword,
            long accountId
    ) {

        BigDecimal comparativeValue = null;
        BigDecimal outputValue = null;

        String error = null;

        boolean rollbackExecuted = false;

        try (Connection connection =
                     DriverManager.getConnection(
                             databaseUrl,
                             databaseUser,
                             databasePassword
                     )) {

            connection.setAutoCommit(false);

            try {

                comparativeValue =
                        findComparativeValue(
                                connection,
                                accountId
                        );

                outputValue =
                        findOutputValue(
                                connection,
                                accountId
                        );

            } catch (Exception exception) {

                error =
                        exception.getMessage();

            } finally {

                try {

                    connection.rollback();

                    rollbackExecuted = true;

                } catch (SQLException rollbackException) {

                    error =
                            appendError(
                                    error,
                                    "ROLLBACK failed: "
                                            + rollbackException.getMessage()
                            );
                }
            }

        } catch (SQLException connectionException) {

            error =
                    appendError(
                            error,
                            "Database error: "
                                    + connectionException.getMessage()
                    );
        }

        boolean valuesMatch =
                error == null
                        && comparativeValue != null
                        && outputValue != null
                        && comparativeValue.compareTo(
                                outputValue
                        ) == 0;

        return new AccountValidationResult(
                accountId,
                comparativeValue,
                outputValue,
                valuesMatch,
                rollbackExecuted,
                error
        );
    }

    private static BigDecimal findComparativeValue(
            Connection connection,
            long accountId
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             CALCULATE_COMPARATIVE_VALUE
                     )) {

            statement.setLong(
                    1,
                    accountId
            );

            statement.setLong(
                    2,
                    accountId
            );

            statement.setLong(
                    3,
                    accountId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {

                    throw new IllegalStateException(
                            "Conta "
                                    + accountId
                                    + " nao encontrada em CONTASCESSAO."
                    );
                }

                BigDecimal value =
                        resultSet.getBigDecimal(
                                "VALOR_COMPARATIVO"
                        );

                if (value == null) {

                    throw new IllegalStateException(
                            "ValorComparativo retornou NULL para conta "
                                    + accountId
                                    + "."
                    );
                }

                if (resultSet.next()) {

                    throw new IllegalStateException(
                            "Mais de um ValorComparativo encontrado para conta "
                                    + accountId
                                    + "."
                    );
                }

                return value;
            }
        }
    }

    private static BigDecimal findOutputValue(
            Connection connection,
            long accountId
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_VALOR_SAIDA
                     )) {

            statement.setLong(
                    1,
                    accountId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {

                    throw new IllegalStateException(
                            "Nenhum registro encontrado em CADOCBaixasEmissor para conta "
                                    + accountId
                                    + "."
                    );
                }

                BigDecimal value =
                        resultSet.getBigDecimal(
                                "ValorSaida"
                        );

                if (value == null) {

                    throw new IllegalStateException(
                            "ValorSaida retornou NULL para conta "
                                    + accountId
                                    + "."
                    );
                }

                if (resultSet.next()) {

                    throw new IllegalStateException(
                            "Mais de um registro encontrado em CADOCBaixasEmissor para conta "
                                    + accountId
                                    + "."
                    );
                }

                return value;
            }
        }
    }

    private static void printResult(
            AccountValidationResult result
    ) {

        System.out.println();

        System.out.println(
                "Conta..................: "
                        + result.accountId
        );

        if (result.comparativeValue != null) {

            System.out.println(
                    "ValorComparativo.......: "
                            + result.comparativeValue.toPlainString()
            );
        }

        if (result.outputValue != null) {

            System.out.println(
                    "ValorSaida.............: "
                            + result.outputValue.toPlainString()
            );
        }

        if (result.comparativeValue != null
                && result.outputValue != null) {

            System.out.println(
                    "Diferenca..............: "
                            + result.comparativeValue
                            .subtract(
                                    result.outputValue
                            )
                            .toPlainString()
            );
        }

        if (result.error != null) {

            System.out.println(
                    "Resultado..............: ERROR"
            );

            System.out.println(
                    "Erro...................: "
                            + result.error
            );

        } else {

            System.out.println(
                    "Resultado..............: "
                            + (result.passed
                            ? "PASS"
                            : "FAIL")
            );
        }

        System.out.println(
                "Rollback................: "
                        + (result.rollbackExecuted
                        ? "OK"
                        : "NAO EXECUTADO")
        );

        System.out.println(
                "----------------------------------------------------------------------"
        );
    }


    private static Set<Long> loadAccounts() {

        String file =
                System.getProperty(
                        "validation.accounts.file"
                );

        // Se recebeu CSV, usa o arquivo.
        if (file != null
                && !file.trim().isEmpty()) {

            java.nio.file.Path path =
                    java.nio.file.Paths.get(
                            file.trim()
                    );

            if (!java.nio.file.Files.exists(path)) {

                throw new IllegalArgumentException(
                        "Arquivo de contas nao encontrado: "
                                + path.toAbsolutePath()
                );
            }

            Set<Long> accounts =
                    new LinkedHashSet<>();

            try {

                java.util.List<String> lines =
                        java.nio.file.Files.readAllLines(
                                path
                        );

                for (String line : lines) {

                    if (line == null) {
                        continue;
                    }

                    String normalized =
                            line
                                    .replace("\uFEFF", "")
                                    .replace("\"", "")
                                    .trim();

                    if (normalized.isEmpty()) {
                        continue;
                    }

                    if (normalized.equalsIgnoreCase("IdConta")
                            || normalized.equalsIgnoreCase("Id_Conta")) {
                        continue;
                    }

                    // Aceita CSV separado por ; ou ,
                    if (normalized.contains(";")) {
                        normalized =
                                normalized
                                        .split(";")[0]
                                        .trim();
                    }

                    if (normalized.contains(",")) {
                        normalized =
                                normalized
                                        .split(",")[0]
                                        .trim();
                    }

                    long accountId;

                    try {

                        accountId =
                                Long.parseLong(
                                        normalized
                                );

                    } catch (NumberFormatException exception) {

                        throw new IllegalArgumentException(
                                "Id_Conta invalido no CSV: "
                                        + normalized,
                                exception
                        );
                    }

                    if (accountId <= 0) {

                        throw new IllegalArgumentException(
                                "Id_Conta deve ser maior que zero: "
                                        + accountId
                        );
                    }

                    accounts.add(
                            accountId
                    );
                }

            } catch (java.io.IOException exception) {

                throw new IllegalStateException(
                        "Nao foi possivel ler o CSV: "
                                + path.toAbsolutePath(),
                        exception
                );
            }

            if (accounts.isEmpty()) {

                throw new IllegalArgumentException(
                        "Nenhuma conta encontrada no CSV: "
                                + path.toAbsolutePath()
                );
            }

            System.out.println();
            System.out.println(
                    "Contas carregadas do CSV.: "
                            + accounts.size()
            );

            return accounts;
        }

        // Continua aceitando o modo antigo por virgula.
        return parseAccounts(
                requireProperty(
                        "validation.accounts"
                )
        );
    }
    private static Set<Long> parseAccounts(
            String rawAccounts
    ) {

        Set<Long> accounts =
                new LinkedHashSet<>();

        String[] values =
                rawAccounts.split(",");

        for (String value : values) {

            String normalized =
                    value.trim();

            if (normalized.isEmpty()) {
                continue;
            }

            long accountId;

            try {

                accountId =
                        Long.parseLong(
                                normalized
                        );

            } catch (NumberFormatException exception) {

                throw new IllegalArgumentException(
                        "Id_Conta invalido: "
                                + normalized,
                        exception
                );
            }

            if (accountId <= 0) {

                throw new IllegalArgumentException(
                        "Id_Conta deve ser maior que zero: "
                                + accountId
                );
            }

            accounts.add(
                    accountId
            );
        }

        if (accounts.isEmpty()) {

            throw new IllegalArgumentException(
                    "Nenhuma conta foi informada."
            );
        }

        return accounts;
    }

    private static String buildDatabaseUrl() {

        String directUrl =
                System.getProperty(
                        "db.url"
                );

        if (directUrl != null
                && !directUrl.trim().isEmpty()) {

            return directUrl.trim();
        }

        String ip =
                requireProperty(
                        "DB_IP"
                );

        String port =
                System.getProperty(
                        "DB_PORT",
                        "1433"
                );

        String database =
                System.getProperty(
                        "DB_NAME",
                        "bmg"
                );

        return "jdbc:sqlserver://"
                + ip
                + ":"
                + port
                + ";databaseName="
                + database
                + ";encrypt=true"
                + ";trustServerCertificate=true";
    }

    private static String requireCredential(
            String systemProperty,
            String environmentVariable
    ) {

        String value =
                System.getProperty(
                        systemProperty
                );

        if (value == null
                || value.trim().isEmpty()) {

            value =
                    System.getenv(
                            environmentVariable
                    );
        }

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + systemProperty
                            + " ou configure "
                            + environmentVariable
                            + "."
            );
        }

        return value;
    }

    private static String requireProperty(
            String name
    ) {

        String value =
                System.getProperty(
                        name
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + name
                            + "=..."
            );
        }

        return value.trim();
    }

    private static String appendError(
            String current,
            String additional
    ) {

        if (current == null
                || current.trim().isEmpty()) {

            return additional;
        }

        return current
                + " | "
                + additional;
    }

    private static final class AccountValidationResult {

        private final long accountId;

        private final BigDecimal comparativeValue;

        private final BigDecimal outputValue;

        private final boolean passed;

        private final boolean rollbackExecuted;

        private final String error;

        private AccountValidationResult(
                long accountId,
                BigDecimal comparativeValue,
                BigDecimal outputValue,
                boolean passed,
                boolean rollbackExecuted,
                String error
        ) {

            this.accountId =
                    accountId;

            this.comparativeValue =
                    comparativeValue;

            this.outputValue =
                    outputValue;

            this.passed =
                    passed;

            this.rollbackExecuted =
                    rollbackExecuted;

            this.error =
                    error;
        }
    }
}