package com.automation.integration.quitacao;

import com.automation.features.quitacao.model.BoletoEmitido;
import com.automation.features.quitacao.repository.BoletoEmitidoRepository;
import com.automation.features.quitacao.validator.BoletoEmitidoValidator;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Epic("Financial Validation Platform")
@Feature("PRE-BATCH")
@Story("Quitacao Antecipada")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public final class QuitacaoPersistenceVerificationIT {

    @Test
    @Description(
            "Valida as evidencias da simulacao e geracao realizadas anteriormente "
                    + "e confirma, em modo READ_ONLY, a persistencia do boleto na base."
    )
    public void shouldValidateExistingSettlementTicketPersistence() throws Exception {

        Allure.getLifecycle().updateTestCase(
                result -> result.setName(
                        "03 - Quitacao Antecipada - Simulacao, Geracao e Persistencia"
                )
        );

        configureAllureHierarchy();

        long accountId = requireLong("quitacao.verify.account");
        long ticketId = requireLong("quitacao.verify.boleto");

        String csvProperty = System.getProperty(
                "quitacao.verify.csv",
                "target/quitacao-mass-results/simulacao-quitacao-mass-real-20261028-185731.csv"
        );

        Path csv = Paths.get(csvProperty);

        Evidence evidence = loadEvidence(
                csv,
                accountId,
                ticketId
        );

        registerParameters(evidence);

        validateSimulation(evidence);
        validateGeneration(evidence);
        validatePersistence(accountId, ticketId);

        attachExecutiveSummary(evidence);

        Allure.addAttachment(
                "Evidencia bruta - CSV da execucao real",
                "text/csv",
                Files.readString(csv, StandardCharsets.UTF_8)
        );
    }

    @Step("01 - Carregar evidencias da execucao real")
    private Evidence loadEvidence(
            Path csv,
            long expectedAccountId,
            long expectedTicketId
    ) throws Exception {

        Assert.assertTrue(
                Files.exists(csv),
                "CSV da execucao real nao encontrado: " + csv
        );

        List<String> lines = Files.readAllLines(
                csv,
                StandardCharsets.UTF_8
        );

        Assert.assertTrue(
                lines.size() >= 2,
                "CSV da quitacao nao possui registros."
        );

        String[] headers = lines.get(0).
                replace("\uFEFF", "").
                split(";", -1);

        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {

            String[] values = lines.get(lineIndex).split(";", -1);

            Map<String, String> row = new LinkedHashMap<>();

            for (int i = 0; i < headers.length && i < values.length; i++) {
                row.put(headers[i].trim(), values[i].trim());
            }

            long accountId = Long.parseLong(row.get("idConta"));
            long ticketId = Long.parseLong(row.get("idBoleto"));

            if (accountId == expectedAccountId
                    && ticketId == expectedTicketId) {

                return Evidence.from(row);
            }
        }

        throw new AssertionError(
                "Nao foi encontrada evidencia para IdConta="
                        + expectedAccountId
                        + " e IdBoleto="
                        + expectedTicketId
        );
    }

    @Step("02 - Validar simulacao da quitacao")
    private void validateSimulation(Evidence evidence) {

        Assert.assertEquals(
                evidence.simulationReturnCode,
                0,
                "A simulacao nao retornou sucesso."
        );

        Assert.assertTrue(
                evidence.totalAntecipacao.compareTo(BigDecimal.ZERO) > 0,
                "TotalAntecipacao deve ser maior que zero."
        );

        Allure.addAttachment(
                "Simulacao - Resultado Financeiro",
                "text/plain",
                evidence.simulationSummary()
        );
    }

    @Step("03 - Validar geracao do boleto de quitacao")
    private void validateGeneration(Evidence evidence) {

        Assert.assertEquals(
                evidence.generationReturnCode,
                0,
                "A geracao do boleto nao retornou sucesso."
        );

        Assert.assertTrue(
                evidence.ticketId > 0,
                "IdBoleto deve ser maior que zero."
        );

        Allure.addAttachment(
                "Geracao - Boleto de Quitacao",
                "text/plain",
                evidence.generationSummary()
        );
    }

    @Step("04 - Confirmar persistencia do boleto em BoletosEmitidos")
    private void validatePersistence(
            long accountId,
            long ticketId
    ) throws Exception {

        Optional<BoletoEmitido> result =
                new BoletoEmitidoRepository()
                        .buscarPorIdContaEIdBoleto(
                                accountId,
                                ticketId
                        );

        new BoletoEmitidoValidator()
                .validar(
                        result,
                        accountId,
                        ticketId
                );
    }

    private void configureAllureHierarchy() {

        Allure.getLifecycle().updateTestCase(result -> {

            result.getLabels().removeIf(label ->
                    "parentSuite".equals(label.getName())
                            || "suite".equals(label.getName())
                            || "subSuite".equals(label.getName())
            );

            result.getLabels().add(
                    new Label()
                            .setName("parentSuite")
                            .setValue("Financial Validation Platform")
            );

            result.getLabels().add(
                    new Label()
                            .setName("suite")
                            .setValue("PRE-BATCH - Smoke V1")
            );

            result.getLabels().add(
                    new Label()
                            .setName("subSuite")
                            .setValue("Quitacao Antecipada")
            );
        });
    }

    private void registerParameters(Evidence evidence) {

        Allure.parameter("Ambiente", System.getProperty("env", "141"));
        Allure.parameter("Modo", "READ_ONLY");
        Allure.parameter("IdConta", evidence.accountId);
        Allure.parameter("IdBoleto", evidence.ticketId);
        Allure.parameter("TotalAntecipacao", evidence.totalAntecipacao);
        Allure.parameter("SimulacaoCodRetorno", evidence.simulationReturnCode);
        Allure.parameter("GeracaoCodRetorno", evidence.generationReturnCode);
    }

    private void attachExecutiveSummary(Evidence evidence) {

        String summary =
                "QUITACAO ANTECIPADA - RESULTADO EXECUTIVO"
                        + System.lineSeparator()
                        + "========================================"
                        + System.lineSeparator()
                        + "Resultado geral........: APROVADO"
                        + System.lineSeparator()
                        + "IdConta................: " + evidence.accountId
                        + System.lineSeparator()
                        + "IdBoleto...............: " + evidence.ticketId
                        + System.lineSeparator()
                        + "Total antecipacao......: R$ " + evidence.totalAntecipacao
                        + System.lineSeparator()
                        + "Simulacao..............: SUCESSO"
                        + System.lineSeparator()
                        + "Geracao................: SUCESSO"
                        + System.lineSeparator()
                        + "Persistencia SQL.......: CONFIRMADA"
                        + System.lineSeparator()
                        + "Registro bancario......: NAO APLICAVEL AO FLUXO";

        Allure.addAttachment(
                "Resumo Executivo - Quitacao Antecipada",
                "text/plain",
                summary
        );
    }

    private static long requireLong(String propertyName) {

        String value = System.getProperty(propertyName);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Propriedade obrigatoria nao informada: "
                            + propertyName
            );
        }

        return Long.parseLong(value.trim());
    }

    private static final class Evidence {

        private final long accountId;
        private final int simulationReturnCode;
        private final String simulationDescription;
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
        private final long ticketId;
        private final int generationReturnCode;
        private final String generationDescription;
        private final String logId;

        private Evidence(
                long accountId,
                int simulationReturnCode,
                String simulationDescription,
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
                BigDecimal totalAntecipacao,
                long ticketId,
                int generationReturnCode,
                String generationDescription,
                String logId
        ) {
            this.accountId = accountId;
            this.simulationReturnCode = simulationReturnCode;
            this.simulationDescription = simulationDescription;
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
            this.ticketId = ticketId;
            this.generationReturnCode = generationReturnCode;
            this.generationDescription = generationDescription;
            this.logId = logId;
        }

        private static Evidence from(Map<String, String> row) {
            return new Evidence(
                    Long.parseLong(row.get("idConta")),
                    Integer.parseInt(row.get("simCodRetorno")),
                    row.get("simDescricao"),
                    decimal(row.get("transacoesDoPeriodo")),
                    decimal(row.get("transacoesAntecipadas")),
                    decimal(row.get("ajusteCredito")),
                    decimal(row.get("ajusteDebito")),
                    decimal(row.get("encargos")),
                    decimal(row.get("iof")),
                    decimal(row.get("saldoFaturaAnterior")),
                    decimal(row.get("totalDesconto")),
                    decimal(row.get("abatimentoJuros")),
                    decimal(row.get("valorPagoMes")),
                    decimal(row.get("totalAntecipacao")),
                    Long.parseLong(row.get("idBoleto")),
                    Integer.parseInt(row.get("geracaoCodRetorno")),
                    row.get("geracaoDescricao"),
                    row.get("idLog")
            );
        }

        private String simulationSummary() {
            return "SIMULACAO DA QUITACAO"
                    + System.lineSeparator()
                    + "------------------------------"
                    + System.lineSeparator()
                    + "IdConta.................: " + accountId
                    + System.lineSeparator()
                    + "CodRetorno..............: " + simulationReturnCode
                    + System.lineSeparator()
                    + "Descricao...............: " + simulationDescription
                    + System.lineSeparator()
                    + "TransacoesPeriodo.......: R$ " + transacoesDoPeriodo
                    + System.lineSeparator()
                    + "TransacoesAntecipadas...: R$ " + transacoesAntecipadas
                    + System.lineSeparator()
                    + "AjusteCredito...........: R$ " + ajusteCredito
                    + System.lineSeparator()
                    + "AjusteDebito............: R$ " + ajusteDebito
                    + System.lineSeparator()
                    + "Encargos................: R$ " + encargos
                    + System.lineSeparator()
                    + "IOF.....................: R$ " + iof
                    + System.lineSeparator()
                    + "SaldoFaturaAnterior.....: R$ " + saldoFaturaAnterior
                    + System.lineSeparator()
                    + "TotalDesconto...........: R$ " + totalDesconto
                    + System.lineSeparator()
                    + "AbatimentoJuros.........: R$ " + abatimentoJuros
                    + System.lineSeparator()
                    + "ValorPagoMes............: R$ " + valorPagoMes
                    + System.lineSeparator()
                    + "TOTAL ANTECIPACAO........: R$ " + totalAntecipacao;
        }

        private String generationSummary() {
            return "GERACAO DO BOLETO"
                    + System.lineSeparator()
                    + "------------------------------"
                    + System.lineSeparator()
                    + "IdBoleto................: " + ticketId
                    + System.lineSeparator()
                    + "CodRetorno..............: " + generationReturnCode
                    + System.lineSeparator()
                    + "Descricao...............: " + generationDescription
                    + System.lineSeparator()
                    + "IdLog...................: " + logId
                    + System.lineSeparator()
                    + "Registro bancario.......: NAO APLICAVEL AO FLUXO";
        }

        private static BigDecimal decimal(String value) {
            return new BigDecimal(value);
        }
    }
}
