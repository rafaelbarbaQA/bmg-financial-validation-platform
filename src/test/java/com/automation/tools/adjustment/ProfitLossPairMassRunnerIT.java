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

public final class ProfitLossPairMassRunnerIT {

    private static final int LUCRO = 1472;
    private static final int PREJUIZO = 1474;

    private static final int DEFAULT_EXPECTED_ACCOUNTS = 50;

    private static final String REQUIRED_DATABASE_HOST =
            "10.213.192.102";

    private static final LocalDateTime DATA_AJUSTE =
            LocalDateTime.of(
                    2026,
                    7,
                    23,
                    11,
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

    private static final String CHECK_DUPLICATE =
            "DECLARE @Responsavel VARCHAR(15) = 'CESSAO-LUCROPREJUIZO'; "
                    + "SELECT COUNT(1) "
                    + "FROM EventosExternosAjustes "
                    + "WHERE Id_Conta = ? "
                    + "AND Id_TipoAjuste = ? "
                    + "AND DataInclusao = ? "
                    + "AND Valor = ? "
                    + "AND ResponsavelInclusao = @Responsavel";

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
    public void runProfitAndLossPairs() throws Exception {

        List<Integer> contas =
                readAccounts(
                        requireAccountFile()
                );

        int expectedAccounts =
                Integer.parseInt(
                        System.getProperty(
                                "adjustment.expected",
                                String.valueOf(
                                        DEFAULT_EXPECTED_ACCOUNTS
                                )
                        )
                );

        if (contas.size() != expectedAccounts) {

            throw new IllegalArgumentException(
                    "Este runner exige exatamente "
                            + expectedAccounts
                            + " contas. Encontradas: "
                            + contas.size()
            );
        }

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

        List<AccountPair> pairs =
                generatePairs(
                        contas,
                        random
                );

        boolean execute =
                Boolean.parseBoolean(
                        System.getProperty(
                                "adjustment.execute",
                                "false"
                        )
                );

        printPreview(
                pairs,
                seed,
                execute
        );

        if (!execute) {

            System.out.println();
            System.out.println(
                    "PREVIEW FINALIZADO - NENHUM INSERT EXECUTADO."
            );

            System.out.println(
                    "Use o mesmo seed na execucao:"
            );

            System.out.println(
                    "-Dadjustment.seed=" + seed
            );

            return;
        }

        String databaseUrl =
                requireProperty("db.url");

        String databaseUser =
                requireProperty("db.user");

        String databasePassword =
                requireProperty("db.password");

        validateTargetDatabase(
                databaseUrl
        );

        
        EffectfulExecutionGuard.exigirAutorizacao();
executePairs(
                pairs,
                databaseUrl,
                databaseUser,
                databasePassword
        );
    }

    private static List<AccountPair> generatePairs(
            List<Integer> contas,
            Random random
    ) {

        List<AccountPair> pairs =
                new ArrayList<AccountPair>();

        for (int index = 0;
             index < contas.size();
             index++) {

            BigDecimal first =
                    VALORES.get(
                            random.nextInt(
                                    VALORES.size()
                            )
                    );

            BigDecimal second;

            do {

                second =
                        VALORES.get(
                                random.nextInt(
                                        VALORES.size()
                                )
                        );

            } while (first.compareTo(second) == 0);

            BigDecimal maior =
                    first.max(second);

            BigDecimal menor =
                    first.min(second);

            boolean lucroMaior;

            /*
             * Garantimos pelo menos um caso de cada.
             * O restante permanece aleatorio.
             */
            if (index == 0) {
                lucroMaior = true;
            }
            else if (index == 1) {
                lucroMaior = false;
            }
            else {
                lucroMaior =
                        random.nextBoolean();
            }

            BigDecimal valorLucro =
                    lucroMaior
                            ? maior
                            : menor;

            BigDecimal valorPrejuizo =
                    lucroMaior
                            ? menor
                            : maior;

            pairs.add(
                    new AccountPair(
                            contas.get(index),
                            valorLucro,
                            valorPrejuizo
                    )
            );
        }

        return pairs;
    }

    private static void executePairs(
            List<AccountPair> pairs,
            String databaseUrl,
            String databaseUser,
            String databasePassword
    ) throws Exception {

        int inserted = 0;
        int duplicated = 0;
        int rejected = 0;
        int errors = 0;

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

                for (AccountPair pair : pairs) {

                    Adjustment lucro =
                            new Adjustment(
                                    pair.accountId,
                                    LUCRO,
                                    pair.profitValue
                            );

                    Adjustment prejuizo =
                            new Adjustment(
                                    pair.accountId,
                                    PREJUIZO,
                                    pair.lossValue
                            );

                    Adjustment[] adjustments =
                            new Adjustment[]{
                                    lucro,
                                    prejuizo
                            };

                    for (Adjustment adjustment : adjustments) {

                        try {

                            if (alreadyExists(
                                    connection,
                                    adjustment
                            )) {

                                duplicated++;

                                System.out.printf(
                                        "SKIP | Conta=%d | Tipo=%s | Valor=%s%n",
                                        adjustment.accountId,
                                        describeType(
                                                adjustment.type
                                        ),
                                        adjustment.value
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
                                        "REJEITADO | Conta=%d | Tipo=%s | linhas=%d%n",
                                        adjustment.accountId,
                                        describeType(
                                                adjustment.type
                                        ),
                                        affected
                                );

                                continue;
                            }

                            inserted++;

                            System.out.printf(
                                    "SUCESSO | Conta=%d | Tipo=%s | Valor=%s%n",
                                    adjustment.accountId,
                                    describeType(
                                            adjustment.type
                                    ),
                                    adjustment.value
                            );

                        } catch (Exception exception) {

                            errors++;

                            System.err.printf(
                                    "ERRO | Conta=%d | Tipo=%s | %s%n",
                                    adjustment.accountId,
                                    describeType(
                                            adjustment.type
                                    ),
                                    exception.getMessage()
                            );
                        }
                    }
                }

                connection.commit();

            } catch (Exception exception) {

                connection.rollback();

                throw exception;
            }
        }

        System.out.println();
        System.out.println(
                "============== RESUMO =============="
        );

        System.out.println(
                "Contas.............: " + pairs.size()
        );

        System.out.println(
                "Ajustes esperados..: " + (pairs.size() * 2)
        );

        System.out.println(
                "Inseridos...........: " + inserted
        );

        System.out.println(
                "Ja existentes......: " + duplicated
        );

        System.out.println(
                "Rejeitados..........: " + rejected
        );

        System.out.println(
                "Erros...............: " + errors
        );

        System.out.println(
                "===================================="
        );
    }

    private static boolean alreadyExists(
            Connection connection,
            Adjustment adjustment
    ) throws Exception {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                CHECK_DUPLICATE
                        )
        ) {

            statement.setInt(
                    1,
                    adjustment.accountId
            );

            statement.setInt(
                    2,
                    adjustment.type
            );

            statement.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            DATA_AJUSTE
                    )
            );

            statement.setBigDecimal(
                    4,
                    adjustment.value
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

            Timestamp timestamp =
                    Timestamp.valueOf(
                            DATA_AJUSTE
                    );

            int parameter = 1;

            statement.setInt(
                    parameter++,
                    adjustment.type
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

    private static void printPreview(
            List<AccountPair> pairs,
            long seed,
            boolean execute
    ) {

        int lucroMaior = 0;
        int prejuizoMaior = 0;

        System.out.println();
        System.out.println(
                "=================================================================="
        );

        System.out.println(
                execute
                        ? " LUCRO + PREJUIZO - EXECUCAO"
                        : " LUCRO + PREJUIZO - PREVIEW"
        );

        System.out.println(
                "=================================================================="
        );

        System.out.println(
                "Destino....: 10.213.192.102"
        );

        System.out.println(
                "Data.......: " + DATA_AJUSTE
        );

        System.out.println(
                "Seed.......: " + seed
        );

        System.out.println(
                "Contas.....: " + pairs.size()
        );

        System.out.println(
                "=================================================================="
        );

        for (AccountPair pair : pairs) {

            String resultado;

            if (pair.profitValue.compareTo(
                    pair.lossValue
            ) > 0) {

                resultado =
                        "LUCRO MAIOR";

                lucroMaior++;

            } else {

                resultado =
                        "PREJUIZO MAIOR";

                prejuizoMaior++;
            }

            System.out.printf(
                    "Conta=%d | Lucro=%s | Prejuizo=%s | %s%n",
                    pair.accountId,
                    pair.profitValue,
                    pair.lossValue,
                    resultado
            );
        }

        System.out.println(
                "=================================================================="
        );

        System.out.println(
                "Lucro maior....: " + lucroMaior
        );

        System.out.println(
                "Prejuizo maior..: " + prejuizoMaior
        );

        System.out.println(
                "Total ajustes...: " + (pairs.size() * 2)
        );

        System.out.println(
                "=================================================================="
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
                    Integer.parseInt(value);

            if (!unique.add(accountId)) {

                throw new IllegalArgumentException(
                        "Conta duplicada: "
                                + accountId
                );
            }

            accounts.add(accountId);
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
                            + "Destino permitido: "
                            + REQUIRED_DATABASE_HOST
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
                    "Informe -D" + name + "=..."
            );
        }

        return value.trim();
    }

    private static String describeType(
            int type
    ) {

        return type == LUCRO
                ? "LUCRO(1472)"
                : "PREJUIZO(1474)";
    }

    private static final class AccountPair {

        private final int accountId;
        private final BigDecimal profitValue;
        private final BigDecimal lossValue;

        private AccountPair(
                int accountId,
                BigDecimal profitValue,
                BigDecimal lossValue
        ) {

            this.accountId = accountId;
            this.profitValue = profitValue;
            this.lossValue = lossValue;
        }
    }

    private static final class Adjustment {

        private final int accountId;
        private final int type;
        private final BigDecimal value;

        private Adjustment(
                int accountId,
                int type,
                BigDecimal value
        ) {

            this.accountId = accountId;
            this.type = type;
            this.value = value;
        }
    }
}