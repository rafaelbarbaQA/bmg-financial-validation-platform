package com.automation.tools.masspurchase;

import com.automation.application.purchase.execution.PurchaseExecutionCoordinator;
import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import com.automation.infrastructure.purchase.execution.PreviewPurchaseExecutor;

import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public final class PurchaseMassRunnerIT {

    private static final String DEFAULT_FILE =
            "src/test/resources/massas/compras-300.csv";

    private static final int DEFAULT_MAXIMUM_ACCOUNTS =
            300;

    @Test
    public void previewPurchaseMass() throws Exception {

        Path massFile =
                Paths.get(
                        System.getProperty(
                                "purchase.mass.file",
                                DEFAULT_FILE
                        )
                );

        LocalDate movementDate =
                LocalDate.parse(
                        requireProperty(
                                "purchase.mass.date"
                        )
                );

        long seed =
                Long.parseLong(
                        System.getProperty(
                                "purchase.mass.seed",
                                "20260723"
                        )
                );

        int maximumAccounts =
                Integer.parseInt(
                        System.getProperty(
                                "purchase.mass.max",
                                String.valueOf(
                                        DEFAULT_MAXIMUM_ACCOUNTS
                                )
                        )
                );

        List<MassEntry> entries =
                loadEntries(
                        massFile
                );

        List<MassEntry> activeEntries =
                activeEntries(
                        entries
                );

        validateMaximumAccounts(
                activeEntries,
                maximumAccounts
        );

        validateDuplicateAccounts(
                activeEntries
        );

        PurchaseInstructionGenerator generator =
                new PurchaseInstructionGenerator(
                        seed,
                        movementDate
                );

        PurchaseExecutionCoordinator coordinator =
                new PurchaseExecutionCoordinator(
                        new PreviewPurchaseExecutor()
                );

        Path resultFile =
                createResultFile();

        int successCount = 0;
        int failureCount = 0;

        Map<Integer, Integer> operationCounts =
                new HashMap<Integer, Integer>();

        Map<String, Integer> installmentProfileCounts =
                new HashMap<String, Integer>();

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " PURCHASE MASS - PREVIEW"
        );
        System.out.println(
                "============================================================"
        );
        System.out.println(
                "Arquivo..................: "
                        + massFile.toAbsolutePath()
        );
        System.out.println(
                "Linhas totais............: "
                        + entries.size()
        );
        System.out.println(
                "Linhas ativas............: "
                        + activeEntries.size()
        );
        System.out.println(
                "Seed.....................: "
                        + seed
        );
        System.out.println(
                "Data movimento...........: "
                        + movementDate
        );
        System.out.println(
                "Banco acessado...........: NAO"
        );
        System.out.println(
                "============================================================"
        );
        System.out.println();

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                resultFile,
                                StandardCharsets.UTF_8
                        )
        ) {
            writer.write(
                    "linha;idConta;idOperacao;"
                            + "valorCompra;numeroParcelas;"
                            + "valorParcela;valorContrato;"
                            + "status;mensagem"
            );

            writer.newLine();
            writer.flush();

            int position = 0;

            for (MassEntry entry : activeEntries) {

                position++;

                PurchaseInstruction instruction =
                        null;

                try {

                    PurchaseOperation operation =
                            PurchaseOperation
                                    .fromOperationId(
                                            entry.operationId
                                    );

                    instruction =
                            generator.generate(
                                    new AccountId(
                                            entry.accountId
                                    ),
                                    operation
                            );

                    coordinator.execute(
                            instruction
                    );

                    successCount++;

                    increment(
                            operationCounts,
                            entry.operationId
                    );

                    increment(
                            installmentProfileCounts,
                            profileKey(
                                    instruction
                            )
                    );

                    printSuccess(
                            position,
                            activeEntries.size(),
                            instruction
                    );

                    writeResult(
                            writer,
                            entry.lineNumber,
                            instruction,
                            "SUCCESS",
                            "PREVIEW"
                    );

                } catch (Exception exception) {

                    failureCount++;

                    printFailure(
                            position,
                            activeEntries.size(),
                            entry,
                            exception
                    );

                    writeFailure(
                            writer,
                            entry,
                            exception
                    );
                }

                /*
                 * Mantem o arquivo atualizado mesmo durante
                 * uma massa grande.
                 */
                writer.flush();
            }
        }

        printSummary(
                entries.size(),
                activeEntries.size(),
                successCount,
                failureCount,
                operationCounts,
                installmentProfileCounts,
                resultFile
        );

        assertEquals(
                failureCount,
                0,
                "Purchase mass preview contains failures. "
                        + "Check result file: "
                        + resultFile.toAbsolutePath()
        );
    }

    private static List<MassEntry> loadEntries(
            Path path
    ) throws IOException {

        if (!Files.exists(path)) {
            throw new IllegalArgumentException(
                    "Purchase mass file not found: "
                            + path.toAbsolutePath()
            );
        }

        List<String> lines =
                Files.readAllLines(
                        path,
                        StandardCharsets.UTF_8
                );

        if (lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "Purchase mass file is empty."
            );
        }

        validateHeader(
                lines.get(0)
        );

        List<MassEntry> entries =
                new ArrayList<MassEntry>();

        for (int index = 1;
             index < lines.size();
             index++) {

            String line =
                    lines.get(index)
                            .trim();

            if (line.isEmpty()
                    || line.startsWith("#")) {
                continue;
            }

            entries.add(
                    parseLine(
                            line,
                            index + 1
                    )
            );
        }

        if (entries.isEmpty()) {
            throw new IllegalArgumentException(
                    "Purchase mass file does not contain data rows."
            );
        }

        return entries;
    }

    private static void validateHeader(
            String header
    ) {

        String normalized =
                header
                        .replace("\uFEFF", "")
                        .trim();

        if (!"idConta;idOperacao;ativo"
                .equalsIgnoreCase(normalized)) {

            throw new IllegalArgumentException(
                    "Invalid CSV header. Expected: "
                            + "idConta;idOperacao;ativo"
                            + ". Received: "
                            + normalized
            );
        }
    }

    private static MassEntry parseLine(
            String line,
            int lineNumber
    ) {

        String[] columns =
                line.split(
                        ";",
                        -1
                );

        if (columns.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid CSV line "
                            + lineNumber
                            + ". Expected 3 columns: "
                            + "idConta;idOperacao;ativo"
            );
        }

        String accountId =
                requireText(
                        columns[0],
                        "idConta",
                        lineNumber
                );

        int operationId;

        try {
            operationId =
                    Integer.parseInt(
                            requireText(
                                    columns[1],
                                    "idOperacao",
                                    lineNumber
                            )
                    );

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Invalid idOperacao at line "
                            + lineNumber
                            + ": "
                            + columns[1],
                    exception
            );
        }

        /*
         * Valida imediatamente se a operacao existe
         * no dominio da automacao.
         */
        PurchaseOperation.fromOperationId(
                operationId
        );

        String activeValue =
                requireText(
                        columns[2],
                        "ativo",
                        lineNumber
                );

        if (!"true".equalsIgnoreCase(activeValue)
                && !"false".equalsIgnoreCase(activeValue)) {

            throw new IllegalArgumentException(
                    "Invalid ativo value at line "
                            + lineNumber
                            + ". Use true or false."
            );
        }

        return new MassEntry(
                lineNumber,
                accountId,
                operationId,
                Boolean.parseBoolean(
                        activeValue
                )
        );
    }

    private static List<MassEntry> activeEntries(
            List<MassEntry> entries
    ) {

        List<MassEntry> active =
                new ArrayList<MassEntry>();

        for (MassEntry entry : entries) {

            if (entry.active) {
                active.add(
                        entry
                );
            }
        }

        if (active.isEmpty()) {
            throw new IllegalArgumentException(
                    "Purchase mass does not contain active accounts."
            );
        }

        return active;
    }

    private static void validateMaximumAccounts(
            List<MassEntry> entries,
            int maximumAccounts
    ) {

        if (maximumAccounts <= 0) {
            throw new IllegalArgumentException(
                    "purchase.mass.max must be greater than zero."
            );
        }

        if (entries.size() > maximumAccounts) {

            throw new IllegalArgumentException(
                    "Purchase mass contains "
                            + entries.size()
                            + " active accounts, but the configured "
                            + "maximum is "
                            + maximumAccounts
                            + "."
            );
        }
    }

    private static void validateDuplicateAccounts(
            List<MassEntry> entries
    ) {

        Set<String> accounts =
                new HashSet<String>();

        for (MassEntry entry : entries) {

            if (!accounts.add(
                    entry.accountId
            )) {

                throw new IllegalArgumentException(
                        "Duplicate active account found in CSV: "
                                + entry.accountId
                                + ". Each account must receive "
                                + "only one movement per mass."
                );
            }
        }
    }

    private static Path createResultFile()
            throws IOException {

        Path directory =
                Paths.get(
                        "target",
                        "purchase-mass-results"
                );

        Files.createDirectories(
                directory
        );

        String timestamp =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        );

        return directory.resolve(
                "purchase-mass-preview-"
                        + timestamp
                        + ".csv"
        );
    }

    private static void printSuccess(
            int position,
            int total,
            PurchaseInstruction instruction
    ) {

        System.out.printf(
                "[%03d/%03d] conta=%s | op=%d | "
                        + "valor=%s | %dx%s | contrato=%s | SUCCESS%n",
                position,
                total,
                instruction
                        .getAccountId()
                        .getValue(),
                instruction
                        .getOperation()
                        .getOperationId(),
                instruction
                        .getPurchaseAmount()
                        .toPlainString(),
                instruction
                        .getInstallmentCount(),
                instruction
                        .getInstallmentAmount()
                        .toPlainString(),
                instruction
                        .getContractAmount()
                        .toPlainString()
        );
    }

    private static void printFailure(
            int position,
            int total,
            MassEntry entry,
            Exception exception
    ) {

        System.out.printf(
                "[%03d/%03d] conta=%s | op=%d | FAILED | %s%n",
                position,
                total,
                entry.accountId,
                entry.operationId,
                safeMessage(
                        exception
                )
        );
    }

    private static void writeResult(
            BufferedWriter writer,
            int lineNumber,
            PurchaseInstruction instruction,
            String status,
            String message
    ) throws IOException {

        writer.write(
                lineNumber
                        + ";"
                        + instruction
                                .getAccountId()
                                .getValue()
                        + ";"
                        + instruction
                                .getOperation()
                                .getOperationId()
                        + ";"
                        + instruction
                                .getPurchaseAmount()
                                .toPlainString()
                        + ";"
                        + instruction
                                .getInstallmentCount()
                        + ";"
                        + instruction
                                .getInstallmentAmount()
                                .toPlainString()
                        + ";"
                        + instruction
                                .getContractAmount()
                                .toPlainString()
                        + ";"
                        + status
                        + ";"
                        + sanitize(
                                message
                        )
        );

        writer.newLine();
    }

    private static void writeFailure(
            BufferedWriter writer,
            MassEntry entry,
            Exception exception
    ) throws IOException {

        writer.write(
                entry.lineNumber
                        + ";"
                        + entry.accountId
                        + ";"
                        + entry.operationId
                        + ";;;;;"
                        + "FAILED"
                        + ";"
                        + sanitize(
                                safeMessage(
                                        exception
                                )
                        )
        );

        writer.newLine();
    }

    private static void printSummary(
            int totalRows,
            int activeRows,
            int successCount,
            int failureCount,
            Map<Integer, Integer> operationCounts,
            Map<String, Integer> profileCounts,
            Path resultFile
    ) {

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " PURCHASE MASS - PREVIEW SUMMARY"
        );
        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Linhas no CSV............: "
                        + totalRows
        );

        System.out.println(
                "Linhas ativas............: "
                        + activeRows
        );

        System.out.println(
                "Sucesso..................: "
                        + successCount
        );

        System.out.println(
                "Falha....................: "
                        + failureCount
        );

        System.out.println(
                "Operacao 19..............: "
                        + count(
                                operationCounts,
                                19
                        )
        );

        System.out.println(
                "Operacao 270.............: "
                        + count(
                                operationCounts,
                                270
                        )
        );

        System.out.println(
                "Operacao 10376...........: "
                        + count(
                                operationCounts,
                                10376
                        )
        );

        System.out.println(
                "Operacao 10436...........: "
                        + count(
                                operationCounts,
                                10436
                        )
        );

        System.out.println(
                "Saque 10436 - 84x........: "
                        + count(
                                profileCounts,
                                "10436-84x"
                        )
        );

        System.out.println(
                "Saque 10436 - 96x........: "
                        + count(
                                profileCounts,
                                "10436-96x"
                        )
        );

        System.out.println(
                "Refi 10376 - 3x..........: "
                        + count(
                                profileCounts,
                                "10376-3x"
                        )
        );

        System.out.println(
                "Refi 10376 - 7x..........: "
                        + count(
                                profileCounts,
                                "10376-7x"
                        )
        );

        System.out.println(
                "Refi 10376 - 12x.........: "
                        + count(
                                profileCounts,
                                "10376-12x"
                        )
        );

        System.out.println(
                "Refi 10376 - 24x.........: "
                        + count(
                                profileCounts,
                                "10376-24x"
                        )
        );

        System.out.println(
                "Refi 10376 - 48x.........: "
                        + count(
                                profileCounts,
                                "10376-48x"
                        )
        );

        System.out.println(
                "Banco alterado...........: NAO"
        );

        System.out.println(
                "Resultado CSV............: "
                        + resultFile.toAbsolutePath()
        );

        System.out.println(
                "============================================================"
        );
    }

    private static String profileKey(
            PurchaseInstruction instruction
    ) {

        return instruction
                .getOperation()
                .getOperationId()
                + "-"
                + instruction
                        .getInstallmentCount()
                + "x";
    }

    private static void increment(
            Map<Integer, Integer> map,
            int key
    ) {

        map.put(
                key,
                count(
                        map,
                        key
                ) + 1
        );
    }

    private static void increment(
            Map<String, Integer> map,
            String key
    ) {

        map.put(
                key,
                count(
                        map,
                        key
                ) + 1
        );
    }

    private static int count(
            Map<Integer, Integer> map,
            int key
    ) {

        Integer value =
                map.get(
                        key
                );

        return value == null
                ? 0
                : value;
    }

    private static int count(
            Map<String, Integer> map,
            String key
    ) {

        Integer value =
                map.get(
                        key
                );

        return value == null
                ? 0
                : value;
    }

    private static String requireProperty(
            String propertyName
    ) {

        String value =
                System.getProperty(
                        propertyName
                );

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + propertyName
                            + "=..."
            );
        }

        return value.trim();
    }

    private static String requireText(
            String value,
            String fieldName,
            int lineNumber
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be empty at CSV line "
                            + lineNumber
                            + "."
            );
        }

        return value.trim();
    }

    private static String safeMessage(
            Exception exception
    ) {

        String message =
                exception.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return exception
                    .getClass()
                    .getSimpleName();
        }

        return message.trim();
    }

    private static String sanitize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        ';',
                        ','
                )
                .replace(
                        '\r',
                        ' '
                )
                .replace(
                        '\n',
                        ' '
                )
                .trim();
    }

    private static final class MassEntry {

        private final int lineNumber;
        private final String accountId;
        private final int operationId;
        private final boolean active;

        private MassEntry(
                int lineNumber,
                String accountId,
                int operationId,
                boolean active
        ) {
            this.lineNumber =
                    lineNumber;

            this.accountId =
                    accountId;

            this.operationId =
                    operationId;

            this.active =
                    active;
        }
    }
}