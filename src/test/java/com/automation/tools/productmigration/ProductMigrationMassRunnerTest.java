package com.automation.tools.productmigration;

import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProductMigrationMassRunnerTest {

    private static final int SOURCE_PRODUCT = 596;
    private static final int TARGET_PRODUCT = 220;

    private static final String API_BASE_URL =
            "https://sandbox-dock.devcdt.com.br";

    private static final String DATABASE_URL =
            "jdbc:sqlserver://10.213.42.50:1433;" +
            "databaseName=bmg;" +
            "encrypt=true;" +
            "trustServerCertificate=true";

    private static final Path MASS_FILE =
            Paths.get(
                    "src",
                    "test",
                    "resources",
                    "massas",
                    "migracao-596-para-220-58.csv"
            );

    private static final Path REPORT_DIR =
            Paths.get(
                    "evidencias",
                    "migracao-produto"
            );

    private static final String BODY =
            "{"
                    + "\"idProduto\":220,"
                    + "\"idOrigemComercial\":1,"
                    + "\"idEstabelecimento\":0,"
                    + "\"idPromotor\":0"
                    + "}";

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

    @Test(groups = {
        "integration",
        "database",
        "effectful"
})
    public void migrateProducts596To220() throws Exception {

        boolean execute =
                Boolean.parseBoolean(
                        System.getProperty(
                                "migration.execute",
                                "false"
                        )
                );

        String confirmation =
                System.getProperty(
                        "migration.confirm",
                        ""
                );

        String databaseUser =
                requireProperty(
                        "db.user"
                );

        String databasePassword =
                requireProperty(
                        "db.password"
                );

        String apiToken =
                execute
                        ? requireProperty("api.token")
                        : System.getProperty("api.token", "");

        if (execute
                && !"SIM".equalsIgnoreCase(confirmation)) {

            throw new IllegalStateException(
                    "Execucao real bloqueada. "
                            + "Use -Dmigration.execute=true "
                            + "-Dmigration.confirm=SIM."
            );
        }

        List<Long> accounts =
                readAccounts(
                        MASS_FILE
                );

        if (accounts.size() != 58) {

            throw new IllegalStateException(
                    "Esperadas 58 contas. Encontradas: "
                            + accounts.size()
            );
        }

        Files.createDirectories(
                REPORT_DIR
        );

        String timestamp =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        );

        Path report =
                REPORT_DIR.resolve(
                        "migracao-596-para-220-"
                                + timestamp
                                + ".csv"
                );

        List<ResultRow> results =
                new ArrayList<>();

        try (
                Connection connection =
                        DriverManager.getConnection(
                                DATABASE_URL,
                                databaseUser,
                                databasePassword
                        )
        ) {

            for (int index = 0;
                 index < accounts.size();
                 index++) {

                long accountId =
                        accounts.get(index);

                int currentProduct =
                        findCurrentProduct(
                                connection,
                                accountId
                        );

                ResultRow result;

                if (currentProduct == -1) {

                    result =
                            ResultRow.rejected(
                                    accountId,
                                    -1,
                                    "CONTA_NAO_ENCONTRADA"
                            );

                } else if (currentProduct == TARGET_PRODUCT) {

                    result =
                            ResultRow.alreadyMigrated(
                                    accountId,
                                    currentProduct
                            );

                } else if (currentProduct != SOURCE_PRODUCT) {

                    result =
                            ResultRow.rejected(
                                    accountId,
                                    currentProduct,
                                    "PRODUTO_ORIGEM_INVALIDO"
                            );

                } else if (!execute) {

                    result =
                            ResultRow.preview(
                                    accountId,
                                    currentProduct
                            );

                } else {

                    result =
                            executeMigration(
                                    connection,
                                    accountId,
                                    currentProduct,
                                    apiToken
                            );
                }

                results.add(
                        result
                );

                System.out.printf(
                        "[%d/%d] Conta=%d | Antes=%s | Depois=%s | HTTP=%s | Status=%s%n",
                        index + 1,
                        accounts.size(),
                        accountId,
                        displayProduct(result.productBefore),
                        displayProduct(result.productAfter),
                        displayHttp(result.httpStatus),
                        result.status
                );
            }
        }

        writeReport(
                report,
                results
        );

        printSummary(
                results,
                execute,
                report
        );
    }

    private static ResultRow executeMigration(
            Connection connection,
            long accountId,
            int productBefore,
            String apiToken
    ) {

        String endpoint =
                API_BASE_URL
                        + "/v2/api/contas/"
                        + accountId
                        + "/alterar-produto";

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            endpoint
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(
                                            30
                                    )
                            )
                            .header(
                                    "accept",
                                    "application/json"
                            )
                            .header(
                                    "access_token",
                                    apiToken
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            BODY,
                                            StandardCharsets.UTF_8
                                    )
                            )
                            .build();

            HttpResponse<String> response =
                    HTTP_CLIENT.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            int httpStatus =
                    response.statusCode();

            String responseBody =
                    clean(
                            response.body()
                    );

            if (httpStatus < 200
                    || httpStatus >= 300) {

                return new ResultRow(
                        accountId,
                        productBefore,
                        TARGET_PRODUCT,
                        null,
                        httpStatus,
                        responseBody,
                        "ERRO_API",
                        "HTTP fora da faixa 2xx"
                );
            }

            int productAfter =
                    waitForProduct(
                            connection,
                            accountId,
                            TARGET_PRODUCT
                    );

            if (productAfter == TARGET_PRODUCT) {

                return new ResultRow(
                        accountId,
                        productBefore,
                        TARGET_PRODUCT,
                        productAfter,
                        httpStatus,
                        responseBody,
                        "SUCESSO",
                        "API 2xx e produto 220 confirmado no banco"
                );
            }

            return new ResultRow(
                    accountId,
                    productBefore,
                    TARGET_PRODUCT,
                    productAfter,
                    httpStatus,
                    responseBody,
                    "NAO_CONFIRMADA",
                    "API retornou 2xx, mas produto 220 nao foi confirmado no banco"
            );

        } catch (Exception exception) {

            return new ResultRow(
                    accountId,
                    productBefore,
                    TARGET_PRODUCT,
                    null,
                    null,
                    "",
                    "ERRO_API",
                    clean(
                            exception.getMessage()
                    )
            );
        }
    }

    private static int waitForProduct(
            Connection connection,
            long accountId,
            int expectedProduct
    ) throws Exception {

        int lastProduct = -1;

        for (int attempt = 1;
             attempt <= 10;
             attempt++) {

            lastProduct =
                    findCurrentProduct(
                            connection,
                            accountId
                    );

            if (lastProduct == expectedProduct) {
                return lastProduct;
            }

            Thread.sleep(
                    1000
            );
        }

        return lastProduct;
    }

    private static int findCurrentProduct(
            Connection connection,
            long accountId
    ) throws Exception {

        String sql =
                "SELECT Id_Produto "
                        + "FROM Contas "
                        + "WHERE Id_Conta = ?";

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                sql
                        )
        ) {

            statement.setLong(
                    1,
                    accountId
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                if (!resultSet.next()) {
                    return -1;
                }

                return resultSet.getInt(
                        "Id_Produto"
                );
            }
        }
    }

    private static List<Long> readAccounts(
            Path path
    ) throws Exception {

        if (!Files.exists(path)) {

            throw new IllegalArgumentException(
                    "Arquivo de massa nao encontrado: "
                            + path.toAbsolutePath()
            );
        }

        Set<Long> unique =
                new LinkedHashSet<>();

        for (
                String line :
                        Files.readAllLines(
                                path,
                                StandardCharsets.UTF_8
                        )
        ) {

            String value =
                    line.trim()
                            .replace(
                                    "\uFEFF",
                                    ""
                            );

            if (value.isEmpty()
                    || value.equalsIgnoreCase(
                            "idConta"
                    )) {
                continue;
            }

            value =
                    value.replace(
                            ",",
                            ""
                    );

            long accountId =
                    Long.parseLong(
                            value
                    );

            if (!unique.add(accountId)) {

                throw new IllegalArgumentException(
                        "Conta duplicada na massa: "
                                + accountId
                );
            }
        }

        return new ArrayList<>(
                unique
        );
    }

    private static void writeReport(
            Path path,
            List<ResultRow> rows
    ) throws Exception {

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                path,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    '\uFEFF'
            );

            writer.write(
                    "idConta;"
                            + "produtoAntes;"
                            + "produtoDestino;"
                            + "httpStatus;"
                            + "respostaApi;"
                            + "produtoDepois;"
                            + "status;"
                            + "observacao"
            );

            writer.newLine();

            for (ResultRow row : rows) {

                writer.write(
                        row.toCsv()
                );

                writer.newLine();
            }
        }
    }

    private static void printSummary(
            List<ResultRow> rows,
            boolean execute,
            Path report
    ) {

        long success =
                countStatus(
                        rows,
                        "SUCESSO"
                );

        long already =
                countStatus(
                        rows,
                        "JA_MIGRADA"
                );

        long preview =
                countStatus(
                        rows,
                        "DRY_RUN"
                );

        long rejected =
                countStatus(
                        rows,
                        "REJEITADA"
                );

        long apiError =
                countStatus(
                        rows,
                        "ERRO_API"
                );

        long notConfirmed =
                countStatus(
                        rows,
                        "NAO_CONFIRMADA"
                );

        System.out.println();
        System.out.println(
                "========== RESUMO MIGRACAO 596 -> 220 =========="
        );
        System.out.println(
                "MODO..............: "
                        + (
                        execute
                                ? "REAL"
                                : "DRY_RUN"
                )
        );
        System.out.println(
                "TOTAL.............: "
                        + rows.size()
        );
        System.out.println(
                "SUCESSO...........: "
                        + success
        );
        System.out.println(
                "JA_MIGRADA........: "
                        + already
        );
        System.out.println(
                "DRY_RUN...........: "
                        + preview
        );
        System.out.println(
                "REJEITADA.........: "
                        + rejected
        );
        System.out.println(
                "ERRO_API..........: "
                        + apiError
        );
        System.out.println(
                "NAO_CONFIRMADA....: "
                        + notConfirmed
        );
        System.out.println(
                "RELATORIO.........: "
                        + report.toAbsolutePath()
        );
        System.out.println(
                "================================================="
        );
    }

    private static long countStatus(
            List<ResultRow> rows,
            String status
    ) {

        return rows.stream()
                .filter(
                        row ->
                                status.equals(
                                        row.status
                                )
                )
                .count();
    }

    private static String requireProperty(
            String property
    ) {

        String value =
                System.getProperty(
                        property
                );

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + property
                            + "=..."
            );
        }

        return value;
    }

    private static String clean(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        '\r',
                        ' '
                )
                .replace(
                        '\n',
                        ' '
                )
                .replace(
                        ';',
                        ','
                )
                .trim();
    }

    private static String displayProduct(
            Integer value
    ) {

        return value == null
                || value < 0
                ? "-"
                : String.valueOf(
                        value
                );
    }

    private static String displayHttp(
            Integer value
    ) {

        return value == null
                ? "-"
                : String.valueOf(
                        value
                );
    }

    private static String csv(
            Object value
    ) {

        if (value == null) {
            return "";
        }

        String text =
                String.valueOf(value)
                        .replace(
                                "\"",
                                "\"\""
                        );

        return "\""
                + text
                + "\"";
    }

    private static final class ResultRow {

        private final long accountId;
        private final int productBefore;
        private final int productTarget;
        private final Integer productAfter;
        private final Integer httpStatus;
        private final String responseBody;
        private final String status;
        private final String observation;

        private ResultRow(
                long accountId,
                int productBefore,
                int productTarget,
                Integer productAfter,
                Integer httpStatus,
                String responseBody,
                String status,
                String observation
        ) {

            this.accountId =
                    accountId;

            this.productBefore =
                    productBefore;

            this.productTarget =
                    productTarget;

            this.productAfter =
                    productAfter;

            this.httpStatus =
                    httpStatus;

            this.responseBody =
                    responseBody;

            this.status =
                    status;

            this.observation =
                    observation;
        }

        private static ResultRow preview(
                long accountId,
                int productBefore
        ) {

            return new ResultRow(
                    accountId,
                    productBefore,
                    TARGET_PRODUCT,
                    productBefore,
                    null,
                    "",
                    "DRY_RUN",
                    "Conta elegivel para migracao 596 -> 220"
            );
        }

        private static ResultRow alreadyMigrated(
                long accountId,
                int productBefore
        ) {

            return new ResultRow(
                    accountId,
                    productBefore,
                    TARGET_PRODUCT,
                    productBefore,
                    null,
                    "",
                    "JA_MIGRADA",
                    "Conta ja esta no produto 220; API nao chamada"
            );
        }

        private static ResultRow rejected(
                long accountId,
                int productBefore,
                String reason
        ) {

            return new ResultRow(
                    accountId,
                    productBefore,
                    TARGET_PRODUCT,
                    productBefore < 0
                            ? null
                            : productBefore,
                    null,
                    "",
                    "REJEITADA",
                    reason
            );
        }

        private String toCsv() {

            return csv(accountId)
                    + ";"
                    + csv(
                            productBefore < 0
                                    ? ""
                                    : productBefore
                    )
                    + ";"
                    + csv(productTarget)
                    + ";"
                    + csv(httpStatus)
                    + ";"
                    + csv(responseBody)
                    + ";"
                    + csv(productAfter)
                    + ";"
                    + csv(status)
                    + ";"
                    + csv(observation);
        }
    }
}
