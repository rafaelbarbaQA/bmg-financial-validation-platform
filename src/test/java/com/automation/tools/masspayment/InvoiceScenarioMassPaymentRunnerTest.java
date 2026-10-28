package com.automation.tools.masspayment;

import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runner otimizado para massa grande.
 *
 * Principais otimizacoes:
 * 1) Massa carregada em #MASSA_CONTAS por batch grande.
 * 2) Boletos + historico + duplicidade retornam em UMA consulta SQL.
 * 3) DRY_RUN nao faz nenhuma consulta SQL por conta.
 * 4) Execucao REAL usa batch JDBC e NOT EXISTS atomico no INSERT.
 * 5) Se um batch REAL falhar, somente aquele batch volta e cai em fallback individual.
 * 6) Progresso e tempo por etapa no console.
 */
public final class InvoiceScenarioMassPaymentRunnerTest {

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 10);
    private static final LocalDateTime PAYMENT_DATE = LocalDateTime.of(2026, 8, 6, 14, 13);

    private static final int TICKET_TYPE = 7;
    private static final int MINIMUM_OPERATION_ID = 10441;
    private static final int AUTHORIZATION_ID = 9999;

    private static final BigDecimal BELOW_MINIMUM_PERCENT = new BigDecimal("0.50");
    private static final BigDecimal ABOVE_MINIMUM_RANGE_PERCENT = new BigDecimal("0.50");
    private static final BigDecimal ABOVE_TOTAL_PERCENT = new BigDecimal("0.10");

    private static final String DEFAULT_ACCOUNTS_FILE =
            "src/test/resources/massas/pagamentos-220-574-20260806.txt";

    private static final String DEFAULT_EXCLUDED_FILE =
            "src/test/resources/massas/contas-sem-pagamento.txt";

    private static final Path REPORT_DIR =
            Paths.get("target", "pagamentos-20260806-220-574");

    private static final Pattern ACCOUNT_PATTERN =
            Pattern.compile("\\b\\d+\\b");

    private static final String CREATE_TEMP =
            "CREATE TABLE #MASSA_CONTAS (ID_CONTA INT NOT NULL PRIMARY KEY)";

    private static final String INSERT_TEMP =
            "INSERT INTO #MASSA_CONTAS (ID_CONTA) VALUES (?)";

    /*
     * A subconsulta PAGAMENTOS_EXISTENTES faz a verificacao de duplicidade
     * em lote no SQL Server. Assim evitamos ~18 mil round-trips Java -> SQL.
     */
    private static final String FIND_ELIGIBLE =
            "SELECT m.ID_CONTA, be.Id_Boleto, be.NossoNumero, " +
            "       be.ValorBoleto, c.Id_Produto, " +
            "       hc.ValorMinimoExtrato, hc.Id_Historico, " +
            "       CASE WHEN pe.ID_CONTA IS NULL THEN 0 ELSE 1 END AS PAGAMENTO_EXISTE " +
            "FROM #MASSA_CONTAS m " +
            "INNER JOIN BoletosEmitidos be ON be.Id_Conta = m.ID_CONTA " +
            "INNER JOIN Contas c ON c.Id_Conta = be.Id_Conta " +
            "INNER JOIN HistoricosCorrentes hc " +
            "        ON hc.Id_Conta = be.Id_Conta " +
            "       AND hc.Id_Historico = be.Id_Historico " +
            "LEFT JOIN ( " +
            "    SELECT ep.ID_CONTA, ep.NOSSONUMERO " +
            "    FROM EVENTOSEXTERNOSPAGAMENTOS ep " +
            "    INNER JOIN #MASSA_CONTAS mx ON mx.ID_CONTA = ep.ID_CONTA " +
            "    GROUP BY ep.ID_CONTA, ep.NOSSONUMERO " +
            ") pe " +
            "       ON pe.ID_CONTA = be.Id_Conta " +
            "      AND pe.NOSSONUMERO = be.NossoNumero " +
            "WHERE be.DataVencimento >= ? " +
            "  AND be.DataVencimento < ? " +
            "  AND c.Id_Produto IN (220, 574) " +
            "  AND be.Id_TipoBoleto = ? " +
            "OPTION (RECOMPILE)";

    private static final String CHECK_DUPLICATED =
            "SELECT TOP (1) 1 " +
            "FROM EVENTOSEXTERNOSPAGAMENTOS " +
            "WHERE ID_CONTA = ? AND NOSSONUMERO = ?";

    /*
     * O NOT EXISTS no proprio INSERT e a ultima trava anti-duplicidade.
     * Elimina a necessidade de SELECT individual antes de cada INSERT.
     */
    private static final String INSERT_PAYMENT =
            "INSERT INTO EVENTOSEXTERNOSPAGAMENTOS (" +
            " BANCO, DATAMOVIMENTO, DATAPAGAMENTO, NOSSONUMERO, VALORPAGO, " +
            " NUMERODOCUMENTO, CARTAO, ID_MOVIMENTOPAGAMENTOPENDENTE, " +
            " DEFASAGEMCOBRANCA, ID_AUTORIZACAO, ID_CONTA, ID_OPERACAO, " +
            " DATAENTRADAPAGAMENTO, DATAVENCIMENTOPADRAO, DATAVENCIMENTOREAL, " +
            " RESTITUICAOSAQUE, STATUS) " +
            "SELECT PROD.BANCO, ?, ?, ?, ?, NULL, C.CARTAO, NULL, NULL, ?, ?, ?, ?, " +
            "       EC.PROXIMOVENCIMENTOPADRAO, EC.PROXIMOVENCIMENTOREAL, 0.00, 0 " +
            "FROM ESTADOSCONTAS EC WITH (NOLOCK) " +
            "INNER JOIN PRODUTOS PROD WITH (NOLOCK) ON PROD.ID_PRODUTO = EC.ID_PRODUTO " +
            "CROSS APPLY (" +
            " SELECT TOP (1) CART.Cartao " +
            " FROM CARTOES CART WITH (NOLOCK) " +
            " WHERE CART.ID_CONTA = EC.ID_CONTA AND CART.PORTADOR = 1 " +
            " ORDER BY CART.SequencialCartao DESC" +
            ") C " +
            "WHERE EC.ID_CONTA = ? " +
            "  AND NOT EXISTS (" +
            "      SELECT 1 " +
            "      FROM EVENTOSEXTERNOSPAGAMENTOS ep " +
            "      WHERE ep.ID_CONTA = ? " +
            "        AND ep.NOSSONUMERO = ?" +
            "  )";

    @Test(groups = {
        "integration",
        "database",
        "effectful"
})
    public void runInvoiceScenarioMassPayments() throws Exception {

        long totalStart = System.nanoTime();

        boolean execute = Boolean.parseBoolean(System.getProperty("execute", "false"));
        int tempBatchSize = intProperty("temp.batch.size", 5000, 100, 20000);
        int realBatchSize = intProperty("real.batch.size", 500, 1, 2000);
        int progressEvery = intProperty("progress.every", 1000, 1, 10000);
        int queryTimeoutSeconds = intProperty("query.timeout.seconds", 600, 0, 3600);
        int accountLimit = intProperty("account.limit", 0, 0, 1000000);

        if (execute && !"SIM".equalsIgnoreCase(System.getProperty("confirm.real", ""))) {
            throw new IllegalStateException(
                    "Execucao real bloqueada. Use -Dexecute=true -Dconfirm.real=SIM"
            );
        }

        String dbUrl = require("db.url");
        String dbUser = require("db.user");
        String dbPassword = require("db.password");

        Path accountsFile = Paths.get(
                System.getProperty("accounts.file", DEFAULT_ACCOUNTS_FILE)
        );

        Path excludedFile = Paths.get(
                System.getProperty("excluded.file", DEFAULT_EXCLUDED_FILE)
        );

        Set<Integer> received = readIds(accountsFile);
        Set<Integer> excludedMaster = readIds(excludedFile);

        Set<Integer> excluded = new LinkedHashSet<>();
        Set<Integer> candidates = new LinkedHashSet<>();

        for (Integer id : received) {
            if (excludedMaster.contains(id)) {
                excluded.add(id);
            } else {
                candidates.add(id);
            }
        }

        List<ResultRow> rows = new ArrayList<>(received.size());

        for (Integer id : excluded) {
            rows.add(ResultRow.excluded(id));
        }

        System.out.println();
        System.out.printf(
                "MASSA | recebidas=%d | semPagamento=%d | candidatas=%d | modo=%s%n",
                received.size(),
                excluded.size(),
                candidates.size(),
                execute ? "REAL" : "DRY_RUN"
        );
        System.out.printf(
                "CONFIG | tempBatch=%d | realBatch=%d | progresso=%d | timeoutSQL=%ds | limite=%s%n",
                tempBatchSize,
                realBatchSize,
                progressEvery,
                queryTimeoutSeconds,
                accountLimit == 0 ? "TODAS" : String.valueOf(accountLimit)
        );
        System.out.flush();

        try (Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            con.setAutoCommit(false);

            long phase = System.nanoTime();
            System.out.println("[1/4] Carregando massa na tabela temporaria...");
            System.out.flush();

            loadTempTable(con, candidates, tempBatchSize, progressEvery);

            System.out.printf(
                    "[1/4] Massa carregada | %s%n",
                    elapsed(phase)
            );
            System.out.flush();

            phase = System.nanoTime();
            System.out.println("[2/4] Consultando boletos + minimo + total + duplicidades EM LOTE...");
            System.out.flush();

            Map<Integer, AccountData> eligible =
                    findEligible(con, queryTimeoutSeconds, progressEvery);

            System.out.printf(
                    "[2/4] Consulta concluida | elegiveis=%d | %s%n",
                    eligible.size(),
                    elapsed(phase)
            );
            System.out.flush();

            for (Integer id : candidates) {
                if (!eligible.containsKey(id)) {
                    rows.add(ResultRow.rejected(
                            id,
                            "SEM_BOLETO_ELEGIVEL"
                    ));
                }
            }

            List<AccountData> accounts = new ArrayList<>(eligible.values());
            Collections.sort(accounts, (a, b) -> Integer.compare(a.accountId, b.accountId));

            if (accountLimit > 0 && accounts.size() > accountLimit) {
                accounts = new ArrayList<>(accounts.subList(0, accountLimit));
                System.out.printf(
                        "LIMITE ATIVO | somente %d contas elegiveis serao processadas nesta execucao.%n",
                        accounts.size()
                );
            }

            // Reserva aproximadamente 20% SEM PAGAMENTO por produto:
            // a cada 5 contas elegiveis do produto 220 e do produto 574,
            // a 5a conta fica sem pagamento.
            List<AccountData> payableAccounts = new ArrayList<>();
            int count220 = 0;
            int count574 = 0;
            int reserved220 = 0;
            int reserved574 = 0;

            for (AccountData account : accounts) {

                boolean reserve = false;

                if (account.productId == 220) {
                    count220++;

                    if (count220 % 5 == 0) {
                        reserve = true;
                        reserved220++;
                    }
                }

                if (account.productId == 574) {
                    count574++;

                    if (count574 % 5 == 0) {
                        reserve = true;
                        reserved574++;
                    }
                }

                if (reserve) {
                    rows.add(ResultRow.excluded(account.accountId));
                } else {
                    payableAccounts.add(account);
                }
            }

            accounts = payableAccounts;

            System.out.printf(
                    "RESERVA 20%% | produto220=%d | produto574=%d | total=%d%n",
                    reserved220,
                    reserved574,
                    reserved220 + reserved574
            );

            phase = System.nanoTime();
            System.out.println("[3/4] Calculando cenarios em memoria...");
            System.out.flush();

            List<PaymentPlan> plans = buildPlans(
                    accounts,
                    rows,
                    execute,
                    progressEvery
            );

            System.out.printf(
                    "[3/4] Planejamento concluido | aptosParaInsert=%d | %s%n",
                    plans.size(),
                    elapsed(phase)
            );
            System.out.flush();

            if (execute) {
                phase = System.nanoTime();
                System.out.printf(
                        "[4/4] Inserindo em batch de %d...%n",
                        realBatchSize
                );
                System.out.flush();

                executeRealBatches(
                        con,
                        plans,
                        rows,
                        realBatchSize,
                        progressEvery,
                        queryTimeoutSeconds
                );

                System.out.printf(
                        "[4/4] Execucao REAL concluida | %s%n",
                        elapsed(phase)
                );
            } else {
                /*
                 * Nenhuma alteracao permanente no DRY_RUN.
                 * A tabela temporaria e todas as leituras sao descartadas.
                 */
                con.rollback();
                System.out.println("[4/4] DRY_RUN concluido | nenhum pagamento inserido.");
            }
        }

        Files.createDirectories(REPORT_DIR);

        writeResults(REPORT_DIR.resolve("resultado-pagamentos.csv"), rows);
        writeExcluded(REPORT_DIR.resolve("contas-sem-pagamento.csv"), excluded);

        printSummary(rows);

        System.out.println(
                "RELATORIO RAW: " +
                        REPORT_DIR.resolve("resultado-pagamentos.csv").toAbsolutePath()
        );
        System.out.println("TEMPO TOTAL: " + elapsed(totalStart));
    }

    private static void loadTempTable(
            Connection con,
            Set<Integer> ids,
            int batchSize,
            int progressEvery
    ) throws Exception {

        /* Statement simples mantem #MASSA_CONTAS no escopo da conexao. */
        try (Statement st = con.createStatement()) {
            st.execute(CREATE_TEMP);
        }

        try (PreparedStatement ps = con.prepareStatement(INSERT_TEMP)) {
            int total = ids.size();
            int currentBatch = 0;
            int processed = 0;

            for (Integer id : ids) {
                ps.setInt(1, id);
                ps.addBatch();

                currentBatch++;
                processed++;

                if (currentBatch >= batchSize) {
                    ps.executeBatch();
                    ps.clearBatch();
                    currentBatch = 0;
                }

                if (processed == total || processed % progressEvery == 0) {
                    printProgress("MASSA SQL", processed, total);
                }
            }

            if (currentBatch > 0) {
                ps.executeBatch();
                ps.clearBatch();
            }
        }
    }

    private static Map<Integer, AccountData> findEligible(
            Connection con,
            int queryTimeoutSeconds,
            int progressEvery
    ) throws Exception {

        Map<Integer, AccountData> result = new LinkedHashMap<>();

        try (PreparedStatement ps = con.prepareStatement(FIND_ELIGIBLE)) {

            if (queryTimeoutSeconds > 0) {
                ps.setQueryTimeout(queryTimeoutSeconds);
            }
            ps.setFetchSize(2000);

            ps.setTimestamp(1, Timestamp.valueOf(DUE_DATE.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(DUE_DATE.plusDays(1).atStartOfDay()));
            ps.setInt(3, TICKET_TYPE);

            try (ResultSet rs = ps.executeQuery()) {
                int read = 0;

                while (rs.next()) {

                    int accountId = rs.getInt("ID_CONTA");

                    if (result.containsKey(accountId)) {
                        throw new IllegalStateException(
                                "Mais de um boleto elegivel para a conta " + accountId
                        );
                    }

                    result.put(
                            accountId,
                            new AccountData(
                                    accountId,
                                    rs.getLong("Id_Boleto"),
                                    trim(rs.getString("NossoNumero")),
                                    rs.getInt("Id_Produto"),
                                    rs.getBigDecimal("ValorBoleto"),
                                    rs.getBigDecimal("ValorMinimoExtrato"),
                                    rs.getLong("Id_Historico"),
                                    rs.getInt("PAGAMENTO_EXISTE") == 1
                            )
                    );

                    read++;
                    if (read % progressEvery == 0) {
                        System.out.printf("CONSULTA SQL | %d registros recebidos...%n", read);
                        System.out.flush();
                    }
                }
            }
        }

        return result;
    }

    private static List<PaymentPlan> buildPlans(
            List<AccountData> accounts,
            List<ResultRow> rows,
            boolean execute,
            int progressEvery
    ) {

        List<PaymentPlan> plans = new ArrayList<>(accounts.size());
        Scenario[] scenarios = Scenario.values();
        int total = accounts.size();

        for (int i = 0; i < total; i++) {

            AccountData account = accounts.get(i);
            Scenario scenario = scenarios[i % scenarios.length];
            int operationId = MINIMUM_OPERATION_ID;

            BigDecimal paymentAmount;

            try {
                validate(account, scenario);
                paymentAmount = calculate(account, scenario);

                // Trava de arredondamento: MAIOR_MINIMO deve continuar
                // estritamente abaixo do TOTAL depois de arredondar para centavos.
                if (scenario == Scenario.MAIOR_MINIMO
                        && paymentAmount.compareTo(
                                account.total.setScale(2, RoundingMode.HALF_UP)
                        ) >= 0) {
                    throw new IllegalArgumentException(
                            "MAIOR_MINIMO arredondou para valor >= ValorBoleto"
                    );
                }
            } catch (RuntimeException ex) {
                rows.add(ResultRow.of(
                        account,
                        scenario,
                        null,
                        operationId,
                        "REJEITADO",
                        clean(ex.getMessage())
                ));
                printMaybeProgress("PROCESSANDO", i + 1, total, progressEvery);
                continue;
            }

            if (account.paymentExists) {
                rows.add(ResultRow.of(
                        account,
                        scenario,
                        paymentAmount,
                        operationId,
                        "JA_EXISTE",
                        "Pagamento ja existe para conta + NossoNumero"
                ));
                printMaybeProgress("PROCESSANDO", i + 1, total, progressEvery);
                continue;
            }

            if (!execute) {
                rows.add(ResultRow.of(
                        account,
                        scenario,
                        paymentAmount,
                        operationId,
                        "DRY_RUN",
                        "Pronto para inserir"
                ));
            } else {
                plans.add(new PaymentPlan(
                        account,
                        scenario,
                        paymentAmount,
                        operationId
                ));
            }

            printMaybeProgress("PROCESSANDO", i + 1, total, progressEvery);
        }

        return plans;
    }

    private static void executeRealBatches(
            Connection con,
            List<PaymentPlan> plans,
            List<ResultRow> rows,
            int batchSize,
            int progressEvery,
            int queryTimeoutSeconds
    ) throws Exception {

        if (plans.isEmpty()) {
            con.rollback();
            System.out.println("Nenhum pagamento apto para insercao.");
            return;
        }

        int total = plans.size();
        int processed = 0;

        try (PreparedStatement ps = con.prepareStatement(INSERT_PAYMENT)) {

            if (queryTimeoutSeconds > 0) {
                ps.setQueryTimeout(queryTimeoutSeconds);
            }

            for (int start = 0; start < total; start += batchSize) {

                int end = Math.min(start + batchSize, total);
                List<PaymentPlan> batch = plans.subList(start, end);
                Savepoint batchSavepoint = con.setSavepoint();

                try {
                    ps.clearBatch();

                    for (PaymentPlan plan : batch) {
                        bindInsert(ps, plan);
                        ps.addBatch();
                    }

                    int[] counts = ps.executeBatch();

                    for (int i = 0; i < batch.size(); i++) {
                        PaymentPlan plan = batch.get(i);
                        int count = i < counts.length ? counts[i] : Statement.SUCCESS_NO_INFO;

                        if (count == 1 || count == Statement.SUCCESS_NO_INFO) {
                            rows.add(ResultRow.of(
                                    plan.account,
                                    plan.scenario,
                                    plan.paymentAmount,
                                    plan.operationId,
                                    "INSERIDO",
                                    "OK_BATCH"
                            ));
                        } else if (count == 0) {
                            classifyZeroInsert(
                                    con,
                                    plan,
                                    rows,
                                    queryTimeoutSeconds
                            );
                        } else if (count == Statement.EXECUTE_FAILED) {
                            throw new BatchUpdateException("Falha reportada pelo driver no batch", counts);
                        } else {
                            rows.add(ResultRow.of(
                                    plan.account,
                                    plan.scenario,
                                    plan.paymentAmount,
                                    plan.operationId,
                                    "REJEITADO",
                                    "INSERT_BATCH retornou " + count
                            ));
                        }
                    }

                    con.commit();

                } catch (Exception batchError) {
                    con.rollback(batchSavepoint);
                    ps.clearBatch();

                    System.out.printf(
                            "BATCH %d-%d falhou (%s). Fallback individual somente neste batch...%n",
                            start + 1,
                            end,
                            clean(batchError.getMessage())
                    );
                    System.out.flush();

                    executeFallbackIndividually(
                            con,
                            batch,
                            rows,
                            queryTimeoutSeconds
                    );
                    con.commit();
                }

                processed = end;
                printMaybeProgress("INSERT REAL", processed, total, progressEvery);
            }
        }
    }

    private static void executeFallbackIndividually(
            Connection con,
            List<PaymentPlan> batch,
            List<ResultRow> rows,
            int queryTimeoutSeconds
    ) throws Exception {

        try (PreparedStatement ps = con.prepareStatement(INSERT_PAYMENT)) {

            if (queryTimeoutSeconds > 0) {
                ps.setQueryTimeout(queryTimeoutSeconds);
            }

            for (PaymentPlan plan : batch) {
                Savepoint sp = con.setSavepoint();

                try {
                    bindInsert(ps, plan);
                    int affected = ps.executeUpdate();

                    if (affected == 1) {
                        rows.add(ResultRow.of(
                                plan.account,
                                plan.scenario,
                                plan.paymentAmount,
                                plan.operationId,
                                "INSERIDO",
                                "OK_FALLBACK"
                        ));
                    } else if (affected == 0) {
                        classifyZeroInsert(
                                con,
                                plan,
                                rows,
                                queryTimeoutSeconds
                        );
                    } else {
                        con.rollback(sp);
                        rows.add(ResultRow.of(
                                plan.account,
                                plan.scenario,
                                plan.paymentAmount,
                                plan.operationId,
                                "REJEITADO",
                                "INSERT retornou " + affected + " linha(s)"
                        ));
                    }

                } catch (Exception ex) {
                    con.rollback(sp);
                    rows.add(ResultRow.of(
                            plan.account,
                            plan.scenario,
                            plan.paymentAmount,
                            plan.operationId,
                            "ERRO",
                            clean(ex.getMessage())
                    ));
                }
            }
        }
    }

    private static void classifyZeroInsert(
            Connection con,
            PaymentPlan plan,
            List<ResultRow> rows,
            int queryTimeoutSeconds
    ) throws Exception {

        boolean duplicate = paymentExists(
                con,
                plan.account.accountId,
                plan.account.nossoNumero,
                queryTimeoutSeconds
        );

        rows.add(ResultRow.of(
                plan.account,
                plan.scenario,
                plan.paymentAmount,
                plan.operationId,
                duplicate ? "JA_EXISTE" : "REJEITADO",
                duplicate
                        ? "Pagamento apareceu entre planejamento e INSERT"
                        : "INSERT retornou 0; verificar ESTADOSCONTAS/PRODUTOS/CARTAO"
        ));
    }

    private static boolean paymentExists(
            Connection con,
            int accountId,
            String nossoNumero,
            int queryTimeoutSeconds
    ) throws Exception {

        try (PreparedStatement ps = con.prepareStatement(CHECK_DUPLICATED)) {

            if (queryTimeoutSeconds > 0) {
                ps.setQueryTimeout(queryTimeoutSeconds);
            }

            ps.setInt(1, accountId);
            ps.setString(2, nossoNumero);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void bindInsert(
            PreparedStatement ps,
            PaymentPlan plan
    ) throws Exception {

        Timestamp paymentTimestamp = Timestamp.valueOf(PAYMENT_DATE);
        int p = 1;

        ps.setTimestamp(p++, paymentTimestamp);
        ps.setTimestamp(p++, paymentTimestamp);
        ps.setString(p++, plan.account.nossoNumero);
        ps.setBigDecimal(p++, plan.paymentAmount);
        ps.setInt(p++, AUTHORIZATION_ID);
        ps.setInt(p++, plan.account.accountId);
        ps.setInt(p++, plan.operationId);
        ps.setTimestamp(p++, paymentTimestamp);
        ps.setInt(p++, plan.account.accountId);

        /* NOT EXISTS anti-duplicidade */
        ps.setInt(p++, plan.account.accountId);
        ps.setString(p, plan.account.nossoNumero);
    }

    private static BigDecimal calculate(AccountData a, Scenario scenario) {

        BigDecimal value;

        switch (scenario) {
            case MINIMO:
                value = a.minimum;
                break;

            case MENOR_MINIMO:
                value = a.minimum.multiply(BELOW_MINIMUM_PERCENT);
                break;

            case MAIOR_MINIMO:
                value = a.minimum.add(
                        a.total.subtract(a.minimum)
                                .multiply(ABOVE_MINIMUM_RANGE_PERCENT)
                );
                break;

            case TOTAL:
                value = a.total;
                break;

            case ACIMA_TOTAL:
                value = a.total.add(
                        a.total.multiply(ABOVE_TOTAL_PERCENT)
                );
                break;

            default:
                throw new IllegalArgumentException("Cenario invalido");
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validate(AccountData a, Scenario scenario) {

        if (a.total == null || a.total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ValorBoleto invalido");
        }

        if (a.minimum == null || a.minimum.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("ValorMinimoExtrato invalido");
        }

        if (a.nossoNumero == null || a.nossoNumero.isBlank()) {
            throw new IllegalArgumentException("NossoNumero vazio");
        }

        if ((scenario == Scenario.MENOR_MINIMO
                || scenario == Scenario.MAIOR_MINIMO)
                && a.minimum.compareTo(a.total) >= 0) {

            throw new IllegalArgumentException(
                    "ValorMinimoExtrato >= ValorBoleto"
            );
        }
    }

    private static Set<Integer> readIds(Path path) throws Exception {

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    "Arquivo nao encontrado: " + path.toAbsolutePath()
            );
        }

        Set<Integer> ids = new LinkedHashSet<>();

        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            Matcher matcher = ACCOUNT_PATTERN.matcher(line);

            if (matcher.find()) {
                ids.add(Integer.parseInt(matcher.group()));
            }
        }

        if (ids.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nenhuma conta encontrada em: " + path.toAbsolutePath()
            );
        }

        return ids;
    }

    private static void writeResults(Path path, List<ResultRow> rows) throws Exception {

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {

            w.write('\uFEFF');
            w.write(
                    "cenario;idConta;idBoleto;idHistorico;idProduto;" +
                    "nossoNumero;valorMinimo;valorTotal;valorPagamento;" +
                    "idOperacao;dataPagamento;dataVencimento;status;mensagem"
            );
            w.newLine();

            for (ResultRow r : rows) {
                w.write(r.toCsv());
                w.newLine();
            }
        }
    }

    private static void writeExcluded(Path path, Set<Integer> excluded) throws Exception {

        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {

            w.write('\uFEFF');
            w.write("idConta;motivo;status");
            w.newLine();

            for (Integer id : excluded) {
                w.write(id + ";CONTA RESERVADA SEM PAGAMENTO;NAO_PROCESSADA");
                w.newLine();
            }
        }
    }

    private static void printSummary(List<ResultRow> rows) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (ResultRow r : rows) {
            String key = (r.scenario == null ? "SEM_CENARIO" : r.scenario.name())
                    + " | " + r.status;

            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        System.out.println();
        System.out.println("========== RESUMO ==========");

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue());
        }

        System.out.println("============================");
    }

    private static String require(String property) {

        String value = System.getProperty(property);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe -D" + property + "=..."
            );
        }

        return value.trim();
    }

    private static int intProperty(
            String property,
            int defaultValue,
            int min,
            int max
    ) {
        String raw = System.getProperty(property);

        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                throw new IllegalArgumentException(
                        "-D" + property + " deve estar entre " + min + " e " + max
                );
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "-D" + property + " deve ser inteiro: " + raw,
                    ex
            );
        }
    }

    private static void printMaybeProgress(
            String label,
            int processed,
            int total,
            int every
    ) {
        if (processed == 1 || processed == total || processed % every == 0) {
            printProgress(label, processed, total);
        }
    }

    private static void printProgress(String label, int processed, int total) {
        double percent = total == 0 ? 100.0 : (processed * 100.0 / total);
        System.out.printf(
                "%s | %d/%d (%.1f%%)%n",
                label,
                processed,
                total,
                percent
        );
        System.out.flush();
    }

    private static String elapsed(long startNanos) {
        Duration d = Duration.ofNanos(System.nanoTime() - startNanos);
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).getSeconds();

        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static String csvValue(Object value) {

        if (value == null) return "";

        String s = value.toString().replace("\"", "\"\"");

        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }

        return s;
    }

    private enum Scenario {
        MINIMO,
        MENOR_MINIMO,
        MAIOR_MINIMO,
        TOTAL,
        ACIMA_TOTAL
    }

    private static final class AccountData {

        final int accountId;
        final long boletoId;
        final String nossoNumero;
        final int productId;
        final BigDecimal total;
        final BigDecimal minimum;
        final long historyId;
        final boolean paymentExists;

        AccountData(
                int accountId,
                long boletoId,
                String nossoNumero,
                int productId,
                BigDecimal total,
                BigDecimal minimum,
                long historyId,
                boolean paymentExists
        ) {
            this.accountId = accountId;
            this.boletoId = boletoId;
            this.nossoNumero = nossoNumero;
            this.productId = productId;
            this.total = total;
            this.minimum = minimum;
            this.historyId = historyId;
            this.paymentExists = paymentExists;
        }
    }

    private static final class PaymentPlan {

        final AccountData account;
        final Scenario scenario;
        final BigDecimal paymentAmount;
        final int operationId;

        PaymentPlan(
                AccountData account,
                Scenario scenario,
                BigDecimal paymentAmount,
                int operationId
        ) {
            this.account = account;
            this.scenario = scenario;
            this.paymentAmount = paymentAmount;
            this.operationId = operationId;
        }
    }

    private static final class ResultRow {

        final Scenario scenario;
        final int accountId;
        final Long boletoId;
        final Long historyId;
        final Integer productId;
        final String nossoNumero;
        final BigDecimal minimum;
        final BigDecimal total;
        final BigDecimal payment;
        final Integer operationId;
        final String status;
        final String message;

        ResultRow(
                Scenario scenario,
                int accountId,
                Long boletoId,
                Long historyId,
                Integer productId,
                String nossoNumero,
                BigDecimal minimum,
                BigDecimal total,
                BigDecimal payment,
                Integer operationId,
                String status,
                String message
        ) {
            this.scenario = scenario;
            this.accountId = accountId;
            this.boletoId = boletoId;
            this.historyId = historyId;
            this.productId = productId;
            this.nossoNumero = nossoNumero;
            this.minimum = minimum;
            this.total = total;
            this.payment = payment;
            this.operationId = operationId;
            this.status = status;
            this.message = message;
        }

        static ResultRow excluded(int accountId) {
            return new ResultRow(
                    null, accountId, null, null, null, null,
                    null, null, null, null,
                    "CONTAS_SEM_PAGAMENTO",
                    "CONTA RESERVADA SEM PAGAMENTO"
            );
        }

        static ResultRow rejected(int accountId, String message) {
            return new ResultRow(
                    null, accountId, null, null, null, null,
                    null, null, null, null,
                    "REJEITADO",
                    message
            );
        }

        static ResultRow of(
                AccountData a,
                Scenario scenario,
                BigDecimal payment,
                int operationId,
                String status,
                String message
        ) {
            return new ResultRow(
                    scenario,
                    a.accountId,
                    a.boletoId,
                    a.historyId,
                    a.productId,
                    a.nossoNumero,
                    a.minimum,
                    a.total,
                    payment,
                    operationId,
                    status,
                    message
            );
        }

        String toCsv() {
            return csvValue(scenario == null ? "" : scenario.name()) + ";" +
                    accountId + ";" +
                    csvValue(boletoId) + ";" +
                    csvValue(historyId) + ";" +
                    csvValue(productId) + ";" +
                    csvValue(nossoNumero) + ";" +
                    csvValue(minimum) + ";" +
                    csvValue(total) + ";" +
                    csvValue(payment) + ";" +
                    csvValue(operationId) + ";" +
                    PAYMENT_DATE + ";" +
                    DUE_DATE + ";" +
                    csvValue(status) + ";" +
                    csvValue(message);
        }
    }
}
