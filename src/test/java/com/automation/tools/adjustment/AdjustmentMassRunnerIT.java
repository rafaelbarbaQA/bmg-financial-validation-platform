package com.automation.tools.adjustment;


import com.automation.core.execution.EffectfulExecutionGuard;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AdjustmentMassRunnerIT {

    private static final int LUCRO = 1472;
    private static final int PREJUIZO = 1474;

    private static final String REQUIRED_DATABASE_HOST =
            "10.213.192.102";

    private static final int COMMIT_BATCH_SIZE = 50;
    private static final int QUERY_TIMEOUT_SECONDS = 15;

    private static final LocalDateTime DATA_AJUSTE =
            LocalDateTime.of(
                    2027,
                    7,
                    23,
                    17,
                    0,
                    0
            );

    private static final List<BigDecimal> VALORES =
            List.of(
                    new BigDecimal("30.00"),
                    new BigDecimal("60.00"),
                    new BigDecimal("120.00"),
                    new BigDecimal("240.00"),
                    new BigDecimal("480.00")
            );

    /*
     * Garante um unico ajuste da nossa carga
     * para a conta na data definida.
     */
    private static final String CHECK_DUPLICATE =
            "DECLARE @Responsavel VARCHAR(15) = 'CESSAO-LUCROPREJUIZO'; "
                    + "SELECT COUNT(1) "
                    + "FROM EventosExternosAjustes "
                    + "WHERE Id_Conta = ? "
                    + "AND DataInclusao = ? "
                    + "AND ResponsavelInclusao = @Responsavel";

    /*
     * Os quatro dados de referencia da automacao sao:
     *
     * Id_Conta
     * Id_TipoAjuste
     * DataInclusao
     * Valor
     *
     * DataOrigem e DataMovimento recebem a mesma DataInclusao,
     * seguindo a query manual fornecida.
     */
    private static final String INSERT_ADJUSTMENT =
            "DECLARE @Responsavel VARCHAR(15) = 'CESSAO-LUCROPREJUIZO'; "

                    + "INSERT INTO EventosExternosAjustes ("
                    + "Id_MovimentoAjuste, "
                    + "Id_TipoAjuste, "
                    + "DataInclusao, "
                    + "DataOrigem, "
                    + "DataMovimento, "
                    + "Id_Conta, "
                    + "Valor, "
                    + "SequencialCartao, "
                    + "DataVencimentoPadrao, "
                    + "DataVencimentoReal, "
                    + "Id_Estabelecimento, "
                    + "Id_TransacaoOriginal, "
                    + "Status, "
                    + "ResponsavelInclusao, "
                    + "Id_EventoExternoOriginal, "
                    + "StatusLojista, "
                    + "TipoEventoExternoOriginal, "
                    + "ValorDestino"
                    + ") "

                    + "SELECT "
                    + "0, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "EC.Id_Conta, "
                    + "?, "
                    + "C.SequencialCartao, "
                    + "EC.ProximoVencimentoPadrao, "
                    + "EC.ProximoVencimentoReal, "
                    + "NULL, "
                    + "NULL, "
                    + "0, "
                    + "@Responsavel, "
                    + "NULL, "
                    + "NULL, "
                    + "NULL, "
                    + "0 "

                    + "FROM ESTADOSCONTAS EC WITH (NOLOCK) "

                    + "CROSS APPLY ("
                    + "SELECT TOP (1) "
                    + "Cartao, "
                    + "SequencialCartao "
                    + "FROM CARTOES C WITH (NOLOCK) "
                    + "WHERE C.Id_Conta = EC.Id_Conta "
                    + "AND C.Portador = 1 "
                    + "ORDER BY C.SequencialCartao DESC"
                    + ") C "

                    + "WHERE EC.Id_Conta = ?";

    @Test(groups = {"effectful"})
    public void runMassAdjustments() throws Exception {

        Path accountFile =
                requireAccountFile();

        List<Integer> accounts =
                readAccounts(accountFile);

        long seed =
                Long.parseLong(
                        System.getProperty(
                                "adjustment.seed",
                                String.valueOf(
                                        System.currentTimeMillis()
                                )
                        )
                );

        Random random =
                new Random(seed);

        List<Adjustment> adjustments =
                generateAdjustments(
                        accounts,
                        random
                );

        boolean execute =
                Boolean.parseBoolean(
                        System.getProperty(
                                "adjustment.execute",
                                "false"
                        )
                );

        printPlan(
                adjustments,
                seed,
                execute
        );

        if (!execute) {

            System.out.println();
            System.out.println(
                    "PREVIEW FINALIZADO - NENHUM INSERT EXECUTADO."
            );

            System.out.println(
                    "Guarde o seed para executar exatamente o mesmo sorteio:"
            );

            System.out.println(
                    "-Dadjustment.seed=" + seed
            );

            return;
        }

        String databaseUrl =
                requireProperty(
                        "db.url"
                );

        String databaseUser =
                requireProperty(
                        "db.user"
                );

        String databasePassword =
                requireProperty(
                        "db.password"
                );

        validateTargetDatabase(
                databaseUrl
        );

        
        EffectfulExecutionGuard.exigirAutorizacao();
executeAdjustments(
                adjustments,
                databaseUrl,
                databaseUser,
                databasePassword
        );
    }

    private static List<Adjustment> generateAdjustments(
            List<Integer> accounts,
            Random random
    ) {

        List<Adjustment> adjustments =
                new ArrayList<Adjustment>();

        for (Integer account : accounts) {

            int adjustmentType =
                    random.nextBoolean()
                            ? LUCRO
                            : PREJUIZO;

            BigDecimal value =
                    VALORES.get(
                            random.nextInt(
                                    VALORES.size()
                            )
                    );

            adjustments.add(
                    new Adjustment(
                            account,
                            adjustmentType,
                            value
                    )
            );
        }

        return adjustments;
    }

    private static void executeAdjustments(
            List<Adjustment> adjustments,
            String databaseUrl,
            String databaseUser,
            String databasePassword
    ) throws Exception {

        int inserted = 0;
        int duplicated = 0;
        int rejected = 0;
        int errors = 0;

        for (int start = 0;
             start < adjustments.size();
             start += COMMIT_BATCH_SIZE) {

            int end =
                    Math.min(
                            start + COMMIT_BATCH_SIZE,
                            adjustments.size()
                    );

            System.out.println();
            System.out.printf(
                    "LOTE | %d a %d de %d%n",
                    start + 1,
                    end,
                    adjustments.size()
            );

            try (
                    Connection connection =
                            DriverManager.getConnection(
                                    databaseUrl,
                                    databaseUser,
                                    databasePassword
                            )
            ) {

                connection.setAutoCommit(false);

                try {

                    for (int index = start;
                         index < end;
                         index++) {

                        Adjustment adjustment =
                                adjustments.get(index);

                        try {

                            if (alreadyExists(
                                    connection,
                                    adjustment.accountId
                            )) {

                                duplicated++;

                                System.out.printf(
                                        "SKIP | Conta=%d | ajuste ja existente em %s%n",
                                        adjustment.accountId,
                                        DATA_AJUSTE
                                );

                                continue;
                            }

                            int affected =
                                    insertAdjustment(
                                            connection,
                                            adjustment
                                    );

                            if (affected != 1) {

                                rejected++;

                                System.err.printf(
                                        "REJEITADO | Conta=%d | linhas afetadas=%d%n",
                                        adjustment.accountId,
                                        affected
                                );

                                continue;
                            }

                            inserted++;

                            System.out.printf(
                                    "SUCESSO | Conta=%d | Tipo=%s | Valor=%s%n",
                                    adjustment.accountId,
                                    describeType(
                                            adjustment.adjustmentType
                                    ),
                                    adjustment.value
                            );

                        } catch (Exception exception) {

                            errors++;

                            System.err.printf(
                                    "ERRO | Conta=%d | %s%n",
                                    adjustment.accountId,
                                    exception.getMessage()
                            );
                        }
                    }

                    connection.commit();

                    System.out.printf(
                            "COMMIT LOTE | %d a %d%n",
                            start + 1,
                            end
                    );

                } catch (Exception exception) {

                    connection.rollback();

                    System.err.println(
                            "ROLLBACK DO LOTE."
                    );

                    throw exception;
                }
            }
        }

        System.out.println();
        System.out.println(
                "============== RESUMO =============="
        );

        System.out.println(
                "Recebidas........: "
                        + adjustments.size()
        );

        System.out.println(
                "Inseridas.........: "
                        + inserted
        );

        System.out.println(
                "Ja existentes.....: "
                        + duplicated
        );

        System.out.println(
                "Rejeitadas.........: "
                        + rejected
        );

        System.out.println(
                "Erros..............: "
                        + errors
        );

        System.out.println(
                "===================================="
        );
    }

    private static boolean alreadyExists(
            Connection connection,
            int accountId
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                CHECK_DUPLICATE
                        )
        ) {

                        statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

statement.setInt(
                    1,
                    accountId
            );

            statement.setTimestamp(
                    2,
                    Timestamp.valueOf(
                            DATA_AJUSTE
                    )
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                resultSet.next();

                return resultSet.getInt(1) > 0;
            }
        }
    }

    private static int insertAdjustment(
            Connection connection,
            Adjustment adjustment
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                INSERT_ADJUSTMENT
                        )
        ) {

                        statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

Timestamp timestamp =
                    Timestamp.valueOf(
                            DATA_AJUSTE
                    );

            int parameter = 1;

            statement.setInt(
                    parameter++,
                    adjustment.adjustmentType
            );

            statement.setTimestamp(
                    parameter++,
                    timestamp
            );

            statement.setTimestamp(
                    parameter++,
                    timestamp
            );

            statement.setTimestamp(
                    parameter++,
                    timestamp
            );

            statement.setBigDecimal(
                    parameter++,
                    adjustment.value
            );

            statement.setInt(
                    parameter,
                    adjustment.accountId
            );

            return statement.executeUpdate();
        }
    }

    private static void printPlan(
            List<Adjustment> adjustments,
            long seed,
            boolean execute
    ) {

        System.out.println();
        System.out.println(
                "=============================================================="
        );

        System.out.println(
                execute
                        ? " AJUSTES - EXECUCAO"
                        : " AJUSTES - PREVIEW"
        );

        System.out.println(
                "=============================================================="
        );

        System.out.println(
                "Destino.....: 10.213.192.102"
        );

        System.out.println(
                "Data........: "
                        + DATA_AJUSTE
        );

        System.out.println(
                "Seed........: "
                        + seed
        );

        System.out.println(
                "Contas......: "
                        + adjustments.size()
        );

        System.out.println(
                "=============================================================="
        );

        for (Adjustment adjustment : adjustments) {

            System.out.printf(
                    "Conta=%d | Tipo=%s | Valor=%s%n",
                    adjustment.accountId,
                    describeType(
                            adjustment.adjustmentType
                    ),
                    adjustment.value
            );
        }

        System.out.println(
                "=============================================================="
        );
    }

    private static List<Integer> readAccounts(
            Path file
    ) throws Exception {

        List<Integer> accounts =
                new ArrayList<Integer>();

        Set<Integer> unique =
                new HashSet<Integer>();

        for (String line :
                Files.readAllLines(file)) {

            String value =
                    line.trim();

            if (value.isEmpty()
                    || value.equalsIgnoreCase("id_conta")
                    || value.equalsIgnoreCase("idConta")) {

                continue;
            }

            value =
                    value.replace(",", "")
                            .replace(";", "");

            int accountId =
                    Integer.parseInt(
                            value
                    );

            if (!unique.add(accountId)) {

                throw new IllegalArgumentException(
                        "Conta duplicada no arquivo: "
                                + accountId
                );
            }

            accounts.add(accountId);
        }

        if (accounts.isEmpty()) {

            throw new IllegalArgumentException(
                    "Arquivo nao possui contas."
            );
        }

        return accounts;
    }

    private static Path requireAccountFile() {

        String value =
                requireProperty(
                        "adjustment.accounts"
                );

        Path path =
                Paths.get(value);

        if (!Files.exists(path)) {

            throw new IllegalArgumentException(
                    "Arquivo nao encontrado: "
                            + path.toAbsolutePath()
            );
        }

        return path;
    }

    private static void validateTargetDatabase(
            String databaseUrl
    ) {

        if (!databaseUrl.contains(
                REQUIRED_DATABASE_HOST
        )) {

            throw new IllegalStateException(
                    "EXECUCAO BLOQUEADA. "
                            + "A automacao de ajustes somente pode executar em "
                            + REQUIRED_DATABASE_HOST
                            + ". URL recebida: "
                            + databaseUrl
            );
        }
    }

    private static String requireProperty(
            String name
    ) {

        String value =
                System.getProperty(name);

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

    private static String describeType(
            int adjustmentType
    ) {

        return adjustmentType == LUCRO
                ? "LUCRO(1472)"
                : "PREJUIZO(1474)";
    }

    private static final class Adjustment {

        private final int accountId;
        private final int adjustmentType;
        private final BigDecimal value;

        private Adjustment(
                int accountId,
                int adjustmentType,
                BigDecimal value
        ) {

            this.accountId =
                    accountId;

            this.adjustmentType =
                    adjustmentType;

            this.value =
                    value;
        }
    }
}