package com.automation.tools.masspurchase;

import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import com.automation.infrastructure.purchase.execution.SqlServerPurchaseExecutor;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PurchaseMassRealTest {

    private static final String REQUIRED_DATABASE_HOST =
            "10.213.42.50";

    private static final String REQUIRED_DATABASE_NAME =
            "BMG";

    @Test(groups = {
        "integration",
        "database",
        "effectful"
})
    public void executarComprasReais() throws Exception {

        boolean execute =
                Boolean.parseBoolean(
                        System.getProperty(
                                "purchase.execute",
                                "false"
                        )
                );

        if (!execute) {
            throw new IllegalStateException(
                    "Execucao REAL bloqueada. Informe "
                            + "-Dpurchase.execute=true."
            );
        }

        String dbIp =
                requireProperty("DB_IP");

        if (!REQUIRED_DATABASE_HOST.equals(dbIp)) {
            throw new IllegalStateException(
                    "BASE BLOQUEADA. Esperado="
                            + REQUIRED_DATABASE_HOST
                            + " | Recebido="
                            + dbIp
            );
        }

        String dbName =
                requireProperty("DB_NAME");

        if (!REQUIRED_DATABASE_NAME.equalsIgnoreCase(dbName)) {
            throw new IllegalStateException(
                    "DATABASE BLOQUEADO. Esperado=BMG"
                            + " | Recebido="
                            + dbName
            );
        }

        String csvFile =
                requireProperty(
                        "purchase.csv"
                );

        int expected =
                Integer.parseInt(
                        requireProperty(
                                "purchase.expected"
                        )
                );

        LocalDate movementDate =
                LocalDate.parse(
                        System.getProperty(
                                "purchase.date",
                                "2026-07-14"
                        )
                );

        long seed =
                Long.parseLong(
                        System.getProperty(
                                "purchase.seed",
                                "20260714"
                        )
                );

        Path csv =
                Paths.get(csvFile);

        if (!Files.exists(csv)) {
            throw new IllegalArgumentException(
                    "CSV nao encontrado: "
                            + csv.toAbsolutePath()
            );
        }

        List<Entry> entries =
                loadEntries(csv);

        if (entries.size() != expected) {
            throw new IllegalStateException(
                    "Quantidade divergente. Esperado="
                            + expected
                            + " | Encontrado="
                            + entries.size()
            );
        }

        /*
         * Última trava antes de qualquer alteração.
         */
        EffectfulExecutionGuard.exigirAutorizacao();

        PurchaseInstructionGenerator generator =
                new PurchaseInstructionGenerator(
                        seed,
                        movementDate
                );

        SqlServerPurchaseExecutor executor =
                new SqlServerPurchaseExecutor();

        Path result =
                Paths.get(
                        System.getProperty(
                                "purchase.result",
                                "target/mass-purchase-real/"
                                        + "resultado-compras-real.csv"
                        )
                );

        Files.createDirectories(
                result.getParent()
        );

        Files.deleteIfExists(result);

        append(
                result,
                "idConta;idOperacao;cenario;valorCompra;"
                        + "parcelas;valorParcela;valorContrato;"
                        + "dataCompra;dataEntrada;status"
        );

        int success = 0;

        System.out.println();
        System.out.println(
                "========== COMPRA REAL =========="
        );
        System.out.println(
                "DB_IP..................: "
                        + dbIp
        );
        System.out.println(
                "Database...............: "
                        + dbName
        );
        System.out.println(
                "Data...................: "
                        + movementDate
        );
        System.out.println(
                "Quantidade..............: "
                        + entries.size()
        );
        System.out.println(
                "================================="
        );

        for (Entry entry : entries) {

            PurchaseOperation operation =
                    PurchaseOperation.fromOperationId(
                            entry.operationId
                    );

            PurchaseInstruction instruction =
                    generator.generate(
                            new AccountId(
                                    entry.accountId
                            ),
                            operation
                    );

            if (!movementDate.equals(
                    instruction.getPurchaseDate()
            )) {
                throw new IllegalStateException(
                        "DataCompra incorreta para "
                                + entry.accountId
                );
            }

            if (!movementDate.equals(
                    instruction.getEntryDate()
            )) {
                throw new IllegalStateException(
                        "DataEntrada incorreta para "
                                + entry.accountId
                );
            }

            try {

                executor.execute(
                        instruction
                );

                success++;

                append(
                        result,
                        row(
                                instruction,
                                "SUCCESS"
                        )
                );

                System.out.printf(
                        "SUCESSO | Conta=%s | Operacao=%d | "
                                + "Valor=%s | Parcelas=%d | "
                                + "Data=%s%n",
                        entry.accountId,
                        entry.operationId,
                        instruction.getPurchaseAmount(),
                        instruction.getInstallmentCount(),
                        instruction.getPurchaseDate()
                );

            } catch (Exception exception) {

                append(
                        result,
                        row(
                                instruction,
                                "ERROR: "
                                        + sanitize(
                                                exception.getMessage()
                                        )
                        )
                );

                throw new IllegalStateException(
                        "Execucao interrompida na conta "
                                + entry.accountId
                                + ". Foram executadas "
                                + success
                                + " compras antes da falha.",
                        exception
                );
            }
        }

        System.out.println();
        System.out.println(
                "========== RESUMO REAL =========="
        );
        System.out.println(
                "Base...................: "
                        + REQUIRED_DATABASE_HOST
        );
        System.out.println(
                "Database...............: BMG"
        );
        System.out.println(
                "Sucesso................: "
                        + success
        );
        System.out.println(
                "Esperado...............: "
                        + expected
        );
        System.out.println(
                "Resultado..............: "
                        + result.toAbsolutePath()
        );
        System.out.println(
                "================================="
        );
    }

    private static List<Entry> loadEntries(
            Path csv
    ) throws Exception {

        List<String> lines =
                Files.readAllLines(
                        csv,
                        StandardCharsets.UTF_8
                );

        List<Entry> entries =
                new ArrayList<>();

        Set<String> accounts =
                new HashSet<>();

        for (String line : lines) {

            String normalized =
                    line
                            .replace("\uFEFF", "")
                            .trim();

            if (normalized.isEmpty()
                    || normalized.equalsIgnoreCase(
                            "idConta;idOperacao;ativo"
                    )) {
                continue;
            }

            String[] columns =
                    normalized.split(";");

            if (columns.length < 3) {
                throw new IllegalArgumentException(
                        "Linha invalida: "
                                + normalized
                );
            }

            String accountId =
                    columns[0].trim();

            int operationId =
                    Integer.parseInt(
                            columns[1].trim()
                    );

            boolean active =
                    Boolean.parseBoolean(
                            columns[2].trim()
                    );

            if (!active) {
                continue;
            }

            if (operationId != 270
                    && operationId != 10557
                    && operationId != 10376) {

                throw new IllegalStateException(
                        "Operacao proibida nesta massa: "
                                + operationId
                );
            }

            if (!accounts.add(accountId)) {
                throw new IllegalStateException(
                        "Conta duplicada: "
                                + accountId
                );
            }

            entries.add(
                    new Entry(
                            accountId,
                            operationId
                    )
            );
        }

        return entries;
    }

    private static String row(
            PurchaseInstruction instruction,
            String status
    ) {

        return instruction.getAccountId().getValue()
                + ";"
                + instruction.getOperation().getOperationId()
                + ";"
                + scenario(
                        instruction
                                .getOperation()
                                .getOperationId()
                )
                + ";"
                + instruction.getPurchaseAmount()
                + ";"
                + instruction.getInstallmentCount()
                + ";"
                + instruction.getInstallmentAmount()
                + ";"
                + instruction.getContractAmount()
                + ";"
                + instruction.getPurchaseDate()
                + ";"
                + instruction.getEntryDate()
                + ";"
                + sanitize(status);
    }

    private static String scenario(
            int operationId
    ) {

        if (operationId == 270) {
            return "A_VISTA_SEM_JUROS";
        }

        if (operationId == 10557) {
            return "PARCELADO_SEM_JUROS_LOJISTA";
        }

        return "PARCELADO_COM_JUROS";
    }

    private static void append(
            Path path,
            String value
    ) throws Exception {

        Files.writeString(
                path,
                value + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private static String sanitize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(';', ',')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
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

    private static final class Entry {

        private final String accountId;
        private final int operationId;

        private Entry(
                String accountId,
                int operationId
        ) {
            this.accountId = accountId;
            this.operationId = operationId;
        }
    }
}