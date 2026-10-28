package com.automation.tools.masspurchase;

import com.automation.application.purchase.generation.PurchaseInstructionGenerator;
import com.automation.domain.account.AccountId;
import com.automation.domain.purchase.PurchaseInstruction;
import com.automation.domain.purchase.PurchaseOperation;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PurchaseMassDryRunTest {

    private static final String REQUIRED_DATABASE_HOST =
            "10.213.42.50";

    @Test(groups = {
        "integration",
        "database"
})
    public void gerarDryRunCompras() throws Exception {

        String csvFile =
                System.getProperty(
                        "purchase.csv",
                        "src/test/resources/massas/compras-20260714.csv"
                );

        String databaseHost =
                System.getProperty(
                        "purchase.db.host",
                        REQUIRED_DATABASE_HOST
                ).trim();

        if (!REQUIRED_DATABASE_HOST.equals(databaseHost)) {
            throw new IllegalStateException(
                    "Base nao autorizada. Esperado: "
                            + REQUIRED_DATABASE_HOST
                            + " | Recebido: "
                            + databaseHost
            );
        }

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

        int expected =
                Integer.parseInt(
                        System.getProperty(
                                "purchase.expected",
                                "94"
                        )
                );

        Path input =
                Paths.get(csvFile);

        if (!Files.exists(input)) {
            throw new IllegalArgumentException(
                    "CSV nao encontrado: "
                            + input.toAbsolutePath()
            );
        }

        List<String> lines =
                Files.readAllLines(
                        input,
                        StandardCharsets.UTF_8
                );

        PurchaseInstructionGenerator generator =
                new PurchaseInstructionGenerator(
                        seed,
                        movementDate
                );

        List<String> output =
                new ArrayList<>();

        output.add(
                "idConta;idOperacao;cenario;valorCompra;parcelas;"
                        + "valorParcela;valorContrato;dataCompra;"
                        + "dataEntrada;databaseHost;modo"
        );

        Set<String> accounts =
                new HashSet<>();

        int total = 0;
        int cash = 0;
        int installmentNoInterest = 0;
        int installmentWithInterest = 0;

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

            String accountValue =
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

            if (!accounts.add(accountValue)) {
                throw new IllegalArgumentException(
                        "Conta duplicada: "
                                + accountValue
                );
            }

            PurchaseOperation operation =
                    PurchaseOperation.fromOperationId(
                            operationId
                    );

            AccountId accountId =
                    new AccountId(
                            accountValue
                    );

            PurchaseInstruction instruction =
                    generator.generate(
                            accountId,
                            operation
                    );

            String scenario =
                    scenario(operationId);

            if (operationId == 270) {
                cash++;
            } else if (operationId == 10557) {
                installmentNoInterest++;
            } else if (operationId == 10376) {
                installmentWithInterest++;
            }

            if (!movementDate.equals(
                    instruction.getPurchaseDate()
            )) {
                throw new IllegalStateException(
                        "DataCompra incorreta para conta "
                                + accountValue
                );
            }

            if (!movementDate.equals(
                    instruction.getEntryDate()
            )) {
                throw new IllegalStateException(
                        "DataEntrada incorreta para conta "
                                + accountValue
                );
            }

            output.add(
                    accountValue
                            + ";"
                            + operationId
                            + ";"
                            + scenario
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
                            + instruction
                                    .getPurchaseDate()
                            + ";"
                            + instruction
                                    .getEntryDate()
                            + ";"
                            + databaseHost
                            + ";DRY_RUN"
            );

            total++;

            System.out.printf(
                    "DRY_RUN | Conta=%s | Operacao=%d | %s | "
                            + "Valor=%s | Parcelas=%d | Parcela=%s | "
                            + "Contrato=%s | Data=%s%n",
                    accountValue,
                    operationId,
                    scenario,
                    instruction.getPurchaseAmount(),
                    instruction.getInstallmentCount(),
                    instruction.getInstallmentAmount(),
                    instruction.getContractAmount(),
                    instruction.getPurchaseDate()
            );
        }

        if (total != expected) {
            throw new IllegalStateException(
                    "Quantidade divergente. Esperado="
                            + expected
                            + " | Gerado="
                            + total
            );
        }

        Path result =
                Paths.get(
                        "target",
                        "mass-purchase-dry-run",
                        "compras-preview-20260714.csv"
                );

        Files.createDirectories(
                result.getParent()
        );

        Files.write(
                result,
                output,
                StandardCharsets.UTF_8
        );

        System.out.println();
        System.out.println(
                "========== DRY RUN COMPRAS =========="
        );
        System.out.println(
                "Base alvo..............: "
                        + databaseHost
        );
        System.out.println(
                "Database...............: BMG"
        );
        System.out.println(
                "Data compra............: "
                        + movementDate
        );
        System.out.println(
                "Data entrada...........: "
                        + movementDate
        );
        System.out.println(
                "Total..................: "
                        + total
        );
        System.out.println(
                "270 - A vista..........: "
                        + cash
        );
        System.out.println(
                "10557 - Parcelado SJ...: "
                        + installmentNoInterest
        );
        System.out.println(
                "10376 - Parcelado CJ...: "
                        + installmentWithInterest
        );
        System.out.println(
                "Alteracoes no banco....: ZERO"
        );
        System.out.println(
                "Preview CSV............: "
                        + result.toAbsolutePath()
        );
        System.out.println(
                "====================================="
        );
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

        if (operationId == 10376) {
            return "PARCELADO_COM_JUROS";
        }

        throw new IllegalArgumentException(
                "Operacao nao permitida nesta massa: "
                        + operationId
        );
    }
}