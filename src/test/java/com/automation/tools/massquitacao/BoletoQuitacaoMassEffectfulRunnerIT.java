package com.automation.tools.massquitacao;

import com.automation.core.config.ConfigReader;
import com.automation.core.execution.EffectfulExecutionGuard;

import com.automation.features.quitacao.service.GerarBoletoQuitacaoAntecipadaService;
import com.automation.features.quitacao.service.SimularQuitacaoAntecipadaService;

import com.automation.features.quitacao.soap.response.GerarBoletoQuitacaoAntecipadaResponse;
import com.automation.features.quitacao.soap.response.SimularQuitacaoAntecipadaResponse;

import com.automation.features.quitacao.testdata.QuitacaoTestConfig;
import com.automation.features.quitacao.validator.SimularQuitacaoAntecipadaValidator;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.IOException;

import java.math.BigDecimal;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertEquals;

@Epic("Quitação")
@Feature("Quitação Antecipada")
@Story("Simulação e geração de boleto")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public final class BoletoQuitacaoMassEffectfulRunnerIT {

    private static final String DEFAULT_FILE =
            "src/test/resources/massas/quitacao-200.csv";

    /*
     * O ambiente da automacao e 141.
     * O WS do 141 foi configurado para trabalhar com a base 102.
     */
    private static final String DEFAULT_EXPECTED_ENVIRONMENT =
            "141";

    private static final int DEFAULT_EXPECTED_ACCOUNTS =
            200;

    private static final int DEFAULT_PARALLELISM =
            4;

    private static final int MAX_PARALLELISM =
            8;

    @Test(groups = { "effectful" })
    @Description(
            "Executa a simulação da quitação antecipada, gera o boleto, " +
            "valida o retorno do serviço e produz evidências da execução."
    )
    public void gerarBoletosQuitacaoEmMassa()
            throws Exception {

        String environment =
                requireProperty(
                        "env"
                );

        String expectedEnvironment =
                System.getProperty(
                        "quitacao.mass.expected.env",
                        DEFAULT_EXPECTED_ENVIRONMENT
                ).trim();

        validateEnvironment(
                environment,
                expectedEnvironment
        );

        Path massFile =
                Paths.get(
                        System.getProperty(
                                "quitacao.mass.file",
                                DEFAULT_FILE
                        )
                );

        int expectedAccounts =
                Integer.parseInt(
                        System.getProperty(
                                "quitacao.mass.expected",
                                String.valueOf(
                                        DEFAULT_EXPECTED_ACCOUNTS
                                )
                        )
                );

        int parallelism =
                Integer.parseInt(
                        System.getProperty(
                                "quitacao.mass.parallelism",
                                String.valueOf(
                                        DEFAULT_PARALLELISM
                                )
                        )
                );

        validateParallelism(
                parallelism
        );

        List<MassEntry> entries =
                loadEntries(
                        massFile
                );

        List<MassEntry> activeEntries =
                activeEntries(
                        entries
                );

        validateExpectedCount(
                activeEntries,
                expectedAccounts
        );

        validateDuplicateAccounts(
                activeEntries
        );

        Allure.getLifecycle()
                .updateTestCase(
                        testResult ->
                                testResult.setName(
                                        "Quitação antecipada - "
                                                + activeEntries.size()
                                                + " conta(s) [Ambiente "
                                                + environment
                                                + "]"
                                )
                );

        Allure.parameter(
                "Ambiente",
                environment
        );

        Allure.parameter(
                "Arquivo de massa",
                massFile.toString()
        );

        Allure.parameter(
                "Contas ativas",
                activeEntries.size()
        );

        Allure.parameter(
                "Paralelismo",
                parallelism
        );

        Allure.step(
                "1. Carregar e validar massa de quitação"
        );

        /*
         * Nenhuma operacao SOAP que possa gerar boleto ocorre
         * antes da autorizacao explicita.
         */
        EffectfulExecutionGuard
                .exigirAutorizacao();

        Allure.step(
                "2. Validar autorização para operação com efeito"
        );

        Allure.parameter(
                "Operação com efeito",
                true
        );

        String wsUrl =
                ConfigReader.getProperty(
                        "WS_URL"
                );

        if (wsUrl == null
                || wsUrl.trim().isEmpty()) {

            throw new IllegalStateException(
                    "WS_URL nao configurada para o ambiente "
                            + environment
                            + "."
            );
        }

        String tipoCorrespondencia =
                QuitacaoTestConfig
                        .getTipoCorrespondencia();

        Allure.parameter(
                "Tipo correspondência",
                tipoCorrespondencia
        );

        Allure.step(
                "3. Validar configuração SOAP do ambiente"
        );

        Path resultFile =
                createResultFile();

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        parallelism
                );

        ExecutorCompletionService<MassOutcome> completionService =
                new ExecutorCompletionService<MassOutcome>(
                        executor
                );

        int successCount = 0;
        int failureCount = 0;
        int registeredCount = 0;
        int notRegisteredCount = 0;

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " SIMULACAO + BOLETO QUITACAO MASS - REAL"
        );
        System.out.println(
                "============================================================"
        );
        System.out.println(
                "Ambiente.................: "
                        + environment
        );
        System.out.println(
                "WS URL...................: "
                        + wsUrl
        );
        System.out.println(
                "Tipo correspondencia.....: "
                        + tipoCorrespondencia
        );
        System.out.println(
                "Arquivo..................: "
                        + massFile.toAbsolutePath()
        );
        System.out.println(
                "Contas ativas............: "
                        + activeEntries.size()
        );
        System.out.println(
                "Paralelismo..............: "
                        + parallelism
        );
        System.out.println(
                "Relatorio.................: "
                        + resultFile.toAbsolutePath()
        );
        System.out.println(
                "Modo.....................: REAL"
        );
        System.out.println(
                "============================================================"
        );
        System.out.println();

        try {

            for (MassEntry entry : activeEntries) {

                final MassEntry currentEntry =
                        entry;

                completionService.submit(
                        () -> executeEntry(
                                currentEntry,
                                tipoCorrespondencia
                        )
                );
            }

            try (
                    BufferedWriter writer =
                            Files.newBufferedWriter(
                                    resultFile,
                                    StandardCharsets.UTF_8
                            )
            ) {

                writer.write(
                        "linha;idConta;"
                                + "simCodRetorno;simDescricao;"
                                + "transacoesDoPeriodo;"
                                + "transacoesAntecipadas;"
                                + "ajusteCredito;ajusteDebito;"
                                + "encargos;iof;"
                                + "saldoFaturaAnterior;"
                                + "totalDesconto;"
                                + "abatimentoJuros;"
                                + "valorPagoMes;"
                                + "totalAntecipacao;"
                                + "idBoleto;"
                                + "geracaoCodRetorno;"
                                + "geracaoDescricao;"
                                + "idLog;registrado;"
                                + "retornoMensagemRegistro;"
                                + "fase;status;mensagem"
                );

                writer.newLine();
                writer.flush();

                for (
                        int completed = 1;
                        completed <= activeEntries.size();
                        completed++
                ) {

                    MassOutcome outcome =
                            completionService
                                    .take()
                                    .get();

                    Allure.step(
                            "4. Executar quitação da conta "
                                    + outcome.entry.accountId
                                    + " - "
                                    + (
                                            outcome.successful
                                                    ? "SUCCESS"
                                                    : "FAILED"
                                    )
                    );

                    if (outcome.successful) {

                        successCount++;

                        if (outcome.registered) {
                            registeredCount++;
                        } else {
                            notRegisteredCount++;
                        }

                        printSuccess(
                                completed,
                                activeEntries.size(),
                                outcome
                        );

                    } else {

                        failureCount++;

                        printFailure(
                                completed,
                                activeEntries.size(),
                                outcome
                        );
                    }

                    writeOutcome(
                            writer,
                            outcome
                    );

                    /*
                     * Checkpoint por conta.
                     * Mesmo se a execucao for interrompida,
                     * os resultados anteriores ficam preservados.
                     */
                    writer.flush();
                }
            }

        } finally {

            executor.shutdownNow();
        }

        printSummary(
                activeEntries.size(),
                successCount,
                failureCount,
                registeredCount,
                notRegisteredCount,
                resultFile
        );

        assertEquals(
                failureCount,
                0,
                "Existem falhas na simulacao/geracao. "
                        + "NAO execute novamente o CSV completo. "
                        + "Utilize somente as contas FAILED do arquivo: "
                        + resultFile.toAbsolutePath()
        );
    }

    private static MassOutcome executeEntry(
            MassEntry entry,
            String tipoCorrespondencia
    ) {

        SimulationData simulationData = null;
        String currentPhase = "SIMULACAO";

        try {

            long accountId =
                    Long.parseLong(
                            entry.accountId
                    );

            /*
             * FASE 1 - SIMULACAO
             */
            SimularQuitacaoAntecipadaService simulationService =
                    new SimularQuitacaoAntecipadaService();

            SimularQuitacaoAntecipadaResponse simulationResponse =
                    simulationService.executar(
                            accountId
                    );

            simulationData =
                    SimulationData.from(
                            simulationResponse
                    );

            /*
             * Reutiliza a validacao de negocio ja existente.
             */
            new SimularQuitacaoAntecipadaValidator()
                    .validar(
                            simulationResponse
                    );

            currentPhase = "GERACAO";

            /*
             * FASE 2 - GERACAO DO BOLETO
             *
             * Somente ocorre depois de uma simulacao valida.
             */
            GerarBoletoQuitacaoAntecipadaService generationService =
                    new GerarBoletoQuitacaoAntecipadaService();

            GerarBoletoQuitacaoAntecipadaResponse generationResponse =
                    generationService.executar(
                            accountId,
                            tipoCorrespondencia
                    );

            long ticketId =
                    generationResponse.getIdBoleto();

            int generationReturnCode =
                    generationResponse.getCodRetorno();

            String generationDescription =
                    generationResponse
                            .getDescricaoRetorno();

            String logId =
                    generationResponse.getIdLog();

            boolean registered =
                    generationResponse.isRegistrado();

            String registrationMessage =
                    generationResponse
                            .getRetornoMensagemRegistro();

            boolean successful =
                    generationReturnCode == 0
                            && ticketId > 0
                            && registered;

            if (!successful) {

                return MassOutcome.generationFailure(
                        entry,
                        simulationData,
                        ticketId,
                        generationReturnCode,
                        generationDescription,
                        logId,
                        registered,
                        registrationMessage
                );
            }

            return MassOutcome.success(
                    entry,
                    simulationData,
                    ticketId,
                    generationReturnCode,
                    generationDescription,
                    logId,
                    registered,
                    registrationMessage
            );

        } catch (AssertionError | Exception exception) {

            return MassOutcome.exceptionFailure(
                    entry,
                    simulationData,
                    currentPhase,
                    exception
            );
        }
    }

    private static List<MassEntry> loadEntries(
            Path path
    ) throws IOException {

        if (!Files.exists(path)) {

            throw new IllegalArgumentException(
                    "Arquivo de massa nao encontrado: "
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
                    "Arquivo de massa vazio."
            );
        }

        validateHeader(
                lines.get(0)
        );

        List<MassEntry> entries =
                new ArrayList<MassEntry>();

        for (
                int index = 1;
                index < lines.size();
                index++
        ) {

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
                    "Arquivo nao possui contas."
            );
        }

        return entries;
    }

    private static void validateHeader(
            String header
    ) {

        String normalized =
                header
                        .replace(
                                "\uFEFF",
                                ""
                        )
                        .trim();

        if (!"idConta;ativo"
                .equalsIgnoreCase(normalized)) {

            throw new IllegalArgumentException(
                    "Cabecalho invalido. Esperado: "
                            + "idConta;ativo"
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

        if (columns.length != 2) {

            throw new IllegalArgumentException(
                    "Linha "
                            + lineNumber
                            + " invalida. Esperado: "
                            + "idConta;ativo"
            );
        }

        String accountId =
                columns[0].trim();

        if (accountId.isEmpty()) {

            throw new IllegalArgumentException(
                    "IdConta vazio na linha "
                            + lineNumber
            );
        }

        try {

            long value =
                    Long.parseLong(
                            accountId
                    );

            if (value <= 0) {

                throw new IllegalArgumentException(
                        "IdConta deve ser maior que zero "
                                + "na linha "
                                + lineNumber
                );
            }

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "IdConta invalido na linha "
                            + lineNumber
                            + ": "
                            + accountId,
                    exception
            );
        }

        String activeText =
                columns[1]
                        .trim();

        if (!"true".equalsIgnoreCase(activeText)
                && !"false".equalsIgnoreCase(activeText)) {

            throw new IllegalArgumentException(
                    "Valor ativo invalido na linha "
                            + lineNumber
                            + ". Use true ou false."
            );
        }

        return new MassEntry(
                lineNumber,
                accountId,
                Boolean.parseBoolean(
                        activeText
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
                    "Nenhuma conta ativa na massa."
            );
        }

        return active;
    }

    private static void validateExpectedCount(
            List<MassEntry> entries,
            int expected
    ) {

        if (expected <= 0) {

            throw new IllegalArgumentException(
                    "quitacao.mass.expected deve ser maior que zero."
            );
        }

        if (entries.size() != expected) {

            throw new IllegalArgumentException(
                    "Quantidade de contas ativas diferente do esperado. "
                            + "Esperado="
                            + expected
                            + ", encontrado="
                            + entries.size()
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
                        "Conta duplicada no CSV: "
                                + entry.accountId
                );
            }
        }
    }

    private static void validateEnvironment(
            String environment,
            String expectedEnvironment
    ) {

        if (!expectedEnvironment.equals(
                environment
        )) {

            throw new IllegalStateException(
                    "Ambiente bloqueado para massa de quitacao. "
                            + "Esperado env="
                            + expectedEnvironment
                            + ", recebido env="
                            + environment
                            + "."
            );
        }
    }

    private static void validateParallelism(
            int parallelism
    ) {

        if (parallelism < 1
                || parallelism > MAX_PARALLELISM) {

            throw new IllegalArgumentException(
                    "quitacao.mass.parallelism deve estar entre 1 e "
                            + MAX_PARALLELISM
                            + "."
            );
        }
    }

    private static Path createResultFile()
            throws IOException {

        Path directory =
                Paths.get(
                        "target",
                        "quitacao-mass-results"
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
                "simulacao-quitacao-mass-real-"
                        + timestamp
                        + ".csv"
        );
    }

    private static void printSuccess(
            int completed,
            int total,
            MassOutcome outcome
    ) {

        System.out.printf(
                "[%03d/%03d] conta=%s | "
                        + "totalAntecipacao=%s | boleto=%d | "
                        + "registrado=%s | SUCCESS%n",
                completed,
                total,
                outcome.entry.accountId,
                value(
                        outcome.simulationData.totalAntecipacao
                ),
                outcome.ticketId,
                outcome.registered
        );
    }

    private static void printFailure(
            int completed,
            int total,
            MassOutcome outcome
    ) {

        System.out.printf(
                "[%03d/%03d] conta=%s | fase=%s | FAILED | %s%n",
                completed,
                total,
                outcome.entry.accountId,
                outcome.phase,
                outcome.message
        );
    }

    private static void writeOutcome(
            BufferedWriter writer,
            MassOutcome outcome
    ) throws IOException {

        SimulationData simulation =
                outcome.simulationData;

        writer.write(
                outcome.entry.lineNumber
                        + ";"
                        + outcome.entry.accountId
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.returnCode
                        )
                        + ";"
                        + sanitize(
                                simulation == null
                                        ? ""
                                        : simulation.returnDescription
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.transacoesDoPeriodo
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.transacoesAntecipadas
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.ajusteCredito
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.ajusteDebito
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.encargos
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.iof
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.saldoFaturaAnterior
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.totalDesconto
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.abatimentoJuros
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.valorPagoMes
                        )
                        + ";"
                        + value(
                                simulation == null
                                        ? null
                                        : simulation.totalAntecipacao
                        )
                        + ";"
                        + value(
                                outcome.ticketId
                        )
                        + ";"
                        + value(
                                outcome.generationReturnCode
                        )
                        + ";"
                        + sanitize(
                                outcome.generationDescription
                        )
                        + ";"
                        + sanitize(
                                outcome.logId
                        )
                        + ";"
                        + (
                                outcome.hasGenerationResponse
                                        ? String.valueOf(
                                                outcome.registered
                                        )
                                        : ""
                        )
                        + ";"
                        + sanitize(
                                outcome.registrationMessage
                        )
                        + ";"
                        + sanitize(
                                outcome.phase
                        )
                        + ";"
                        + (
                                outcome.successful
                                        ? "SUCCESS"
                                        : "FAILED"
                        )
                        + ";"
                        + sanitize(
                                outcome.message
                        )
        );

        writer.newLine();
    }

    private static void printSummary(
            int total,
            int success,
            int failure,
            int registered,
            int notRegistered,
            Path resultFile
    ) {

        System.out.println();
        System.out.println(
                "============================================================"
        );
        System.out.println(
                " SIMULACAO + QUITACAO MASS - SUMMARY"
        );
        System.out.println(
                "============================================================"
        );
        System.out.println(
                "Contas...................: "
                        + total
        );
        System.out.println(
                "Sucesso..................: "
                        + success
        );
        System.out.println(
                "Falha....................: "
                        + failure
        );
        System.out.println(
                "Registrados..............: "
                        + registered
        );
        System.out.println(
                "Nao registrados..........: "
                        + notRegistered
        );
        System.out.println(
                "Resultado CSV............: "
                        + resultFile.toAbsolutePath()
        );
        System.out.println(
                "============================================================"
        );
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

    private static String rootCauseMessage(
            Throwable throwable
    ) {

        Throwable current =
                throwable;

        while (current.getCause() != null) {

            current =
                    current.getCause();
        }

        String message =
                current.getMessage();

        if (message == null
                || message.trim().isEmpty()) {

            return current
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

    private static String value(
            Long value
    ) {

        return value == null
                ? ""
                : String.valueOf(
                        value
                );
    }

    private static String value(
            Integer value
    ) {

        return value == null
                ? ""
                : String.valueOf(
                        value
                );
    }

    private static String value(
            BigDecimal value
    ) {

        return value == null
                ? ""
                : value.toPlainString();
    }

    private static final class MassEntry {

        private final int lineNumber;
        private final String accountId;
        private final boolean active;

        private MassEntry(
                int lineNumber,
                String accountId,
                boolean active
        ) {

            this.lineNumber =
                    lineNumber;

            this.accountId =
                    accountId;

            this.active =
                    active;
        }
    }

    private static final class SimulationData {

        private final Integer returnCode;
        private final String returnDescription;

        private final BigDecimal transacoesDoPeriodo;
        private final BigDecimal transacoesAntecipadas;
        private final BigDecimal ajusteCredito;
        private final BigDecimal ajusteDebito;
        private final BigDecimal encargos;
        private final BigDecimal iof;
        private final BigDecimal saldoFaturaAnterior;
        private final BigDecimal totalDesconto;
        private final BigDecimal abatimentoJuros;
        private final BigDecimal valorPagoMes;
        private final BigDecimal totalAntecipacao;

        private SimulationData(
                Integer returnCode,
                String returnDescription,
                BigDecimal transacoesDoPeriodo,
                BigDecimal transacoesAntecipadas,
                BigDecimal ajusteCredito,
                BigDecimal ajusteDebito,
                BigDecimal encargos,
                BigDecimal iof,
                BigDecimal saldoFaturaAnterior,
                BigDecimal totalDesconto,
                BigDecimal abatimentoJuros,
                BigDecimal valorPagoMes,
                BigDecimal totalAntecipacao
        ) {

            this.returnCode = returnCode;
            this.returnDescription = returnDescription;
            this.transacoesDoPeriodo = transacoesDoPeriodo;
            this.transacoesAntecipadas = transacoesAntecipadas;
            this.ajusteCredito = ajusteCredito;
            this.ajusteDebito = ajusteDebito;
            this.encargos = encargos;
            this.iof = iof;
            this.saldoFaturaAnterior = saldoFaturaAnterior;
            this.totalDesconto = totalDesconto;
            this.abatimentoJuros = abatimentoJuros;
            this.valorPagoMes = valorPagoMes;
            this.totalAntecipacao = totalAntecipacao;
        }

        private static SimulationData from(
                SimularQuitacaoAntecipadaResponse response
        ) {

            return new SimulationData(
                    response.getCodRetorno(),
                    response.getDescricaoRetorno(),
                    response.getTransacoesDoPeriodo(),
                    response.getTransacoesAntecipadas(),
                    response.getAjusteCredito(),
                    response.getAjusteDebito(),
                    response.getEncargos(),
                    response.getIof(),
                    response.getSaldoFaturaAnterior(),
                    response.getTotalDesconto(),
                    response.getAbatimentoJuros(),
                    response.getValorPagoMes(),
                    response.getTotalAntecipacao()
            );
        }
    }

    private static final class MassOutcome {

        private final MassEntry entry;

        private final SimulationData simulationData;

        private final Long ticketId;
        private final Integer generationReturnCode;
        private final String generationDescription;

        private final String logId;
        private final boolean registered;
        private final String registrationMessage;

        private final boolean hasGenerationResponse;
        private final boolean successful;

        private final String phase;
        private final String message;

        private MassOutcome(
                MassEntry entry,
                SimulationData simulationData,
                Long ticketId,
                Integer generationReturnCode,
                String generationDescription,
                String logId,
                boolean registered,
                String registrationMessage,
                boolean hasGenerationResponse,
                boolean successful,
                String phase,
                String message
        ) {

            this.entry = entry;
            this.simulationData = simulationData;
            this.ticketId = ticketId;
            this.generationReturnCode = generationReturnCode;
            this.generationDescription = generationDescription;
            this.logId = logId;
            this.registered = registered;
            this.registrationMessage = registrationMessage;
            this.hasGenerationResponse = hasGenerationResponse;
            this.successful = successful;
            this.phase = phase;
            this.message = message;
        }

        private static MassOutcome success(
                MassEntry entry,
                SimulationData simulationData,
                long ticketId,
                int generationReturnCode,
                String generationDescription,
                String logId,
                boolean registered,
                String registrationMessage
        ) {

            return new MassOutcome(
                    entry,
                    simulationData,
                    ticketId,
                    generationReturnCode,
                    generationDescription,
                    logId,
                    registered,
                    registrationMessage,
                    true,
                    true,
                    "GERACAO",
                    "Simulacao concluida e boleto de quitacao gerado."
            );
        }

        private static MassOutcome generationFailure(
                MassEntry entry,
                SimulationData simulationData,
                long ticketId,
                int generationReturnCode,
                String generationDescription,
                String logId,
                boolean registered,
                String registrationMessage
        ) {

            return new MassOutcome(
                    entry,
                    simulationData,
                    ticketId,
                    generationReturnCode,
                    generationDescription,
                    logId,
                    registered,
                    registrationMessage,
                    true,
                    false,
                    "GERACAO",
                    "Resposta de negocio sem sucesso: "
                            + generationDescription
            );
        }

        private static MassOutcome exceptionFailure(
                MassEntry entry,
                SimulationData simulationData,
                String phase,
                Throwable exception
        ) {

            return new MassOutcome(
                    entry,
                    simulationData,
                    null,
                    null,
                    "",
                    "",
                    false,
                    "",
                    false,
                    false,
                    phase,
                    rootCauseMessage(
                            exception
                    )
            );
        }
    }
}

