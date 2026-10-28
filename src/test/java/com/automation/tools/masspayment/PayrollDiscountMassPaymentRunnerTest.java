package com.automation.tools.masspayment;

import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PayrollDiscountMassPaymentRunnerTest {

    private static final LocalDate DUE_DATE =
            LocalDate.of(2026, 7, 10);

    private static final LocalDateTime PAYMENT_DATE =
            LocalDateTime.of(2026, 7, 10, 14, 13, 0);

    private static final LocalDateTime PREVIOUS_PAYMENT_DATE =
            LocalDateTime.of(2026, 7, 10, 14, 13, 0);

    private static final int OPERATION_ID = 10441;
    private static final int TICKET_TYPE = 7;
    private static final int AUTHORIZATION_ID = 9999;
    private static final int COMMIT_BATCH_SIZE = 500;

    private static final String DEFAULT_CSV_FILE =
            "src/test/resources/massas/pagamentos-desconto-folha.csv";

    private static final String FIND_PAYROLL_DISCOUNT =
            "SELECT DESCONTOFOLHA " +
            "FROM CONTROLEVENCIMENTOSPRODUTOSCONTAS " +
            "WHERE ID_CONTA = ? " +
            "  AND DataVencimento = ?";

    private static final String FIND_INVOICE_TICKET =
            "SELECT TOP (1) " +
            "       Id_Boleto, " +
            "       NossoNumero " +
            "FROM BoletosEmitidos " +
            "WHERE Id_TipoBoleto = ? " +
            "  AND Id_Conta = ? " +
            "  AND DataVencimento = ? " +
            "ORDER BY Id_Boleto DESC";

    private static final String FIND_ALREADY_PAID_ACCOUNTS =
            "SELECT DISTINCT ID_CONTA " +
            "FROM EVENTOSEXTERNOSPAGAMENTOS " +
            "WHERE ID_OPERACAO = ? " +
            "  AND DATAPAGAMENTO IN (?, ?)";

    private static final String CHECK_DUPLICATED_PAYMENT =
            "SELECT COUNT(1) " +
            "FROM EVENTOSEXTERNOSPAGAMENTOS " +
            "WHERE ID_CONTA = ? " +
            "  AND NOSSONUMERO = ?";

    private static final String INSERT_PAYMENT =
            "INSERT INTO EVENTOSEXTERNOSPAGAMENTOS (" +
            "    BANCO, " +
            "    DATAMOVIMENTO, " +
            "    DATAPAGAMENTO, " +
            "    NOSSONUMERO, " +
            "    VALORPAGO, " +
            "    NUMERODOCUMENTO, " +
            "    CARTAO, " +
            "    ID_MOVIMENTOPAGAMENTOPENDENTE, " +
            "    DEFASAGEMCOBRANCA, " +
            "    ID_AUTORIZACAO, " +
            "    ID_CONTA, " +
            "    ID_OPERACAO, " +
            "    DATAENTRADAPAGAMENTO, " +
            "    DATAVENCIMENTOPADRAO, " +
            "    DATAVENCIMENTOREAL, " +
            "    RESTITUICAOSAQUE, " +
            "    STATUS" +
            ") " +
            "SELECT " +
            "    PROD.BANCO, " +
            "    ?, " +
            "    ?, " +
            "    ?, " +
            "    ?, " +
            "    NULL, " +
            "    C.CARTAO, " +
            "    NULL, " +
            "    NULL, " +
            "    ?, " +
            "    ?, " +
            "    ?, " +
            "    ?, " +
            "    EC.PROXIMOVENCIMENTOPADRAO, " +
            "    EC.PROXIMOVENCIMENTOREAL, " +
            "    0.00, " +
            "    0 " +
            "FROM ESTADOSCONTAS EC WITH (NOLOCK) " +
            "INNER JOIN PRODUTOS PROD WITH (NOLOCK) " +
            "        ON PROD.ID_PRODUTO = EC.ID_PRODUTO " +
            "CROSS APPLY (" +
            "    SELECT TOP (1) CART.Cartao " +
            "    FROM CARTOES CART WITH (NOLOCK) " +
            "    WHERE CART.ID_CONTA = EC.ID_CONTA " +
            "      AND CART.PORTADOR = 1 " +
            "    ORDER BY CART.SequencialCartao DESC" +
            ") C " +
            "WHERE EC.ID_CONTA = ?";

    @Test(groups = {
        "integration",
        "database",
        "effectful"
})
    public void runPayrollDiscountMassPayments() throws Exception {
        boolean execute = Boolean.parseBoolean(
                System.getProperty("execute", "false")
        );

        String csvFile = System.getProperty(
                "csv.file",
                DEFAULT_CSV_FILE
        );

        if (System.getProperty("db.url") == null ||
                System.getProperty("db.url").isBlank()) {

            throw new org.testng.SkipException(
                    "Operational mass-payment runner skipped: -Ddb.url was not provided."
            );
        }
        String databaseUrl = requireProperty("db.url");
        String databaseUser = requireProperty("db.user");
        String databasePassword = requireProperty("db.password");

        List<Integer> accountIds = readAccountIds(
                Paths.get(csvFile)
        );

        String resumeFromAccountProperty =
                System.getProperty("resume.from.account");

        if (resumeFromAccountProperty != null
                && !resumeFromAccountProperty.trim().isEmpty()) {

            int resumeFromAccount =
                    Integer.parseInt(
                            resumeFromAccountProperty.trim()
                    );

            int resumeIndex =
                    accountIds.indexOf(resumeFromAccount);

            if (resumeIndex < 0) {
                throw new IllegalArgumentException(
                        "Conta de retomada nao encontrada no CSV: "
                                + resumeFromAccount
                );
            }

            accountIds =
                    new ArrayList<Integer>(
                            accountIds.subList(
                                    resumeIndex,
                                    accountIds.size()
                            )
                    );

            System.out.printf(
                    "RETOMADA | Conta inicial=%d | Contas restantes=%d.%n",
                    resumeFromAccount,
                    accountIds.size()
            );
        }


        boolean exportRemaining =
                Boolean.parseBoolean(
                        System.getProperty(
                                "export.remaining",
                                "false"
                        )
                );

        if (exportRemaining) {

            Path outputFile =
                    Paths.get(
                            "target",
                            "pagamentos-restantes.csv"
                    );

            List<String> outputLines =
                    new ArrayList<String>();

            outputLines.add("idConta");

            for (Integer accountId : accountIds) {
                outputLines.add(
                        String.valueOf(accountId)
                );
            }

            Files.createDirectories(
                    outputFile.getParent()
            );

            Files.write(
                    outputFile,
                    outputLines,
                    StandardCharsets.UTF_8
            );

            System.out.println();
            System.out.printf(
                    "ARQUIVO GERADO | Restantes=%d | %s%n",
                    accountIds.size(),
                    outputFile.toAbsolutePath()
            );

            return;
        }

        printExecutionHeader(
                execute,
                csvFile,
                accountIds.size()
        );

        ExecutionSummary summary = new ExecutionSummary(
                accountIds.size()
        );

        for (int start = 0;
             start < accountIds.size();
             start += COMMIT_BATCH_SIZE) {

            int end = Math.min(
                    start + COMMIT_BATCH_SIZE,
                    accountIds.size()
            );

            System.out.printf(
                    "LOTE | contas %d a %d de %d.%n",
                    start + 1,
                    end,
                    accountIds.size()
            );

            try (Connection connection =
                         DriverManager.getConnection(
                                 databaseUrl,
                                 databaseUser,
                                 databasePassword
                         )) {

                connection.setAutoCommit(false);

                for (int index = start;
                     index < end;
                     index++) {

                    if (connection.isClosed()) {
                        throw new SQLException(
                                "Database connection closed during batch."
                        );
                    }

                    int accountId =
                            accountIds.get(index);

                    processAccount(
                            connection,
                            accountId,
                            execute,
                            summary
                    );

                    if (connection.isClosed()) {
                        throw new SQLException(
                                "Database connection closed during batch."
                        );
                    }
                }

                if (execute) {
                    connection.commit();

                    System.out.printf(
                            "COMMIT LOTE | contas %d a %d | total inserido=%d.%n",
                            start + 1,
                            end,
                            summary.inserted
                    );
                } else {
                    connection.rollback();
                }

            } catch (SQLException exception) {

                System.err.printf(
                        "ERRO DE CONEXAO | Lote %d a %d | %s%n",
                        start + 1,
                        end,
                        exception.getMessage()
                );

                System.err.println(
                        "EXECUCAO INTERROMPIDA. Reexecute o mesmo CSV para continuar com seguranca."
                );

                throw exception;
            }
        }

        summary.print();
    }

    private Set<Integer> findAlreadyPaidAccounts(
            String databaseUrl,
            String databaseUser,
            String databasePassword
    ) throws SQLException {

        Set<Integer> accounts =
                new HashSet<Integer>();

        try (Connection connection =
                     DriverManager.getConnection(
                             databaseUrl,
                             databaseUser,
                             databasePassword
                     );

             PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_ALREADY_PAID_ACCOUNTS
                     )) {

            statement.setInt(
                    1,
                    OPERATION_ID
            );

            statement.setTimestamp(
                    2,
                    Timestamp.valueOf(
                            PREVIOUS_PAYMENT_DATE
                    )
            );

            statement.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            PAYMENT_DATE
                    )
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {
                    accounts.add(
                            resultSet.getInt("ID_CONTA")
                    );
                }
            }
        }

        return accounts;
    }

    private void processAccount(
            Connection connection,
            int accountId,
            boolean execute,
            ExecutionSummary summary
    ) {
        try {
            BigDecimal payrollDiscount =
                    findPayrollDiscount(
                            connection,
                            accountId
                    );

            if (payrollDiscount == null) {
                reject(
                        accountId,
                        "DESCONTOFOLHA nÃ£o encontrado.",
                        summary
                );
                return;
            }

            if (payrollDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                reject(
                        accountId,
                        "DESCONTOFOLHA deve ser maior que zero: "
                                + payrollDiscount,
                        summary
                );
                return;
            }

            InvoiceTicket ticket = findInvoiceTicket(
                    connection,
                    accountId
            );

            if (ticket == null) {
                reject(
                        accountId,
                        "Boleto tipo 7 com vencimento em "
                                + DUE_DATE
                                + " nÃ£o encontrado.",
                        summary
                );
                return;
            }

            if (paymentAlreadyExists(connection, accountId, ticket.nossoNumero)) {
                reject(
                        accountId,
                        "Pagamento jÃ¡ existe para o NossoNumero "
                                + ticket.nossoNumero
                                + ".",
                        summary
                );
                return;
            }

            if (!execute) {
                System.out.printf(
                        "DRY_RUN | Conta=%d | DESCONTOFOLHA=%s "
                                + "| IdBoleto=%s | NossoNumero=%s%n",
                        accountId,
                        payrollDiscount,
                        ticket.ticketId,
                        ticket.nossoNumero
                );

                summary.ready++;
                return;
            }

            int affectedRows = insertPayment(
                    connection,
                    accountId,
                    ticket.nossoNumero,
                    payrollDiscount
            );

            if (affectedRows != 1) {
                reject(
                        accountId,
                        "Insert deveria afetar 1 linha, mas afetou "
                                + affectedRows
                                + ".",
                        summary
                );
                return;
            }

            System.out.printf(
                    "SUCESSO | Conta=%d | Valor=%s "
                            + "| NossoNumero=%s%n",
                    accountId,
                    payrollDiscount,
                    ticket.nossoNumero
            );

            summary.inserted++;

        } catch (Exception exception) {
            System.err.printf(
                    "ERRO | Conta=%d | %s%n",
                    accountId,
                    exception.getMessage()
            );

            summary.errors++;
        }
    }

    private BigDecimal findPayrollDiscount(
            Connection connection,
            int accountId
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_PAYROLL_DISCOUNT
                     )) {

            statement.setInt(1, accountId);
            statement.setTimestamp(
                    2,
                    Timestamp.valueOf(
                            DUE_DATE.atStartOfDay()
                    )
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                BigDecimal value =
                        resultSet.getBigDecimal(
                                "DESCONTOFOLHA"
                        );

                if (resultSet.next()) {
                    throw new IllegalStateException(
                            "Mais de um DESCONTOFOLHA encontrado "
                                    + "para a mesma conta e vencimento."
                    );
                }

                return value;
            }
        }
    }

    private InvoiceTicket findInvoiceTicket(
            Connection connection,
            int accountId
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             FIND_INVOICE_TICKET
                     )) {

            statement.setInt(1, TICKET_TYPE);
            statement.setInt(2, accountId);
            statement.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            DUE_DATE.atStartOfDay()
                    )
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                String ticketId =
                        resultSet.getString("Id_Boleto");

                String nossoNumero =
                        resultSet.getString("NossoNumero");

                if (nossoNumero == null
                        || nossoNumero.trim().isEmpty()) {

                    throw new IllegalStateException(
                            "NossoNumero estÃ¡ nulo ou vazio."
                    );
                }

                return new InvoiceTicket(
                        ticketId,
                        nossoNumero.trim()
                );
            }
        }
    }
    private boolean paymentAlreadyExists(
            Connection connection,
            int accountId,
            String nossoNumero
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             CHECK_DUPLICATED_PAYMENT
                     )) {

            statement.setInt(
                    1,
                    accountId
            );

            statement.setString(
                    2,
                    nossoNumero
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                resultSet.next();

                return resultSet.getInt(1) > 0;
            }
        }
    }

    private int insertPayment(
            Connection connection,
            int accountId,
            String nossoNumero,
            BigDecimal value
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             INSERT_PAYMENT
                     )) {

            Timestamp paymentTimestamp =
                    Timestamp.valueOf(PAYMENT_DATE);

            int parameter = 1;

            statement.setTimestamp(
                    parameter++,
                    paymentTimestamp
            );

            statement.setTimestamp(
                    parameter++,
                    paymentTimestamp
            );

            statement.setString(
                    parameter++,
                    nossoNumero
            );

            statement.setBigDecimal(
                    parameter++,
                    value
            );

            statement.setInt(
                    parameter++,
                    AUTHORIZATION_ID
            );

            statement.setInt(
                    parameter++,
                    accountId
            );

            statement.setInt(
                    parameter++,
                    OPERATION_ID
            );

            statement.setTimestamp(
                    parameter++,
                    paymentTimestamp
            );

            statement.setInt(
                    parameter,
                    accountId
            );

            return statement.executeUpdate();
        }
    }

    private static List<Integer> readAccountIds(
            Path csvPath
    ) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException(
                    "Arquivo CSV nÃ£o encontrado: "
                            + csvPath.toAbsolutePath()
            );
        }

        List<String> lines =
                Files.readAllLines(csvPath);

        Set<Integer> uniqueAccounts =
                new HashSet<Integer>();

        List<Integer> accountIds =
                new ArrayList<Integer>();

        for (String line : lines) {
            String normalized = line.replace("\uFEFF", "").trim();

            if (normalized.isEmpty()
                    || normalized.equalsIgnoreCase("idConta")) {
                continue;
            }

            int accountId;

            try {
                accountId = Integer.parseInt(normalized);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "IdConta invÃ¡lido no CSV: "
                                + normalized,
                        exception
                );
            }

            if (!uniqueAccounts.add(accountId)) {
                throw new IllegalArgumentException(
                        "Conta duplicada no CSV: "
                                + accountId
                );
            }

            accountIds.add(accountId);
        }

        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "O arquivo CSV nÃ£o possui contas."
            );
        }

        return accountIds;
    }

    private static String requireProperty(
            String propertyName
    ) {
        String value = System.getProperty(propertyName);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Informe a propriedade Maven -D"
                            + propertyName
                            + "=..."
            );
        }

        return value.trim();
    }

    private static void reject(
            int accountId,
            String reason,
            ExecutionSummary summary
    ) {
        System.err.printf(
                "REJEITADO | Conta=%d | %s%n",
                accountId,
                reason
        );

        summary.rejected++;
    }

    private static void printExecutionHeader(
            boolean execute,
            String csvFile,
            int quantity
    ) {
        System.out.println();
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "PAGAMENTOS EM MASSA â€” DESCONTO FOLHA"
        );
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "Modo.............: "
                        + (execute ? "EXECUTE" : "DRY_RUN")
        );
        System.out.println(
                "CSV..............: " + csvFile
        );
        System.out.println(
                "Contas...........: " + quantity
        );
        System.out.println(
                "Vencimento.......: " + DUE_DATE
        );
        System.out.println(
                "Data pagamento...: " + PAYMENT_DATE
        );
        System.out.println(
                "Id operaÃ§Ã£o......: " + OPERATION_ID
        );
        System.out.println(
                "Tipo boleto......: " + TICKET_TYPE
        );
        System.out.println(
                "=========================================="
        );
        System.out.println();
    }

    private static final class InvoiceTicket {

        private final String ticketId;
        private final String nossoNumero;

        private InvoiceTicket(
                String ticketId,
                String nossoNumero
        ) {
            this.ticketId = ticketId;
            this.nossoNumero = nossoNumero;
        }
    }

    private static final class ExecutionSummary {

        private final int received;
        private int ready;
        private int inserted;
        private int rejected;
        private int errors;

        private ExecutionSummary(int received) {
            this.received = received;
        }

        private void print() {
            System.out.println();
            System.out.println(
                    "=============== RESUMO ================="
            );
            System.out.println(
                    "Contas recebidas.: " + received
            );
            System.out.println(
                    "Prontas no dry-run: " + ready
            );
            System.out.println(
                    "Inseridas.........: " + inserted
            );
            System.out.println(
                    "Rejeitadas........: " + rejected
            );
            System.out.println(
                    "Erros.............: " + errors
            );
            System.out.println(
                    "========================================"
            );
        }
    }
}



























