package com.automation.tools.masspayment;

import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.scenario.FinancialScenarioPaymentMapper;
import com.automation.core.execution.EffectfulExecutionGuard;
import com.automation.domain.account.AccountId;
import com.automation.domain.financial.scenario.MinimumPaymentScenario;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.infrastructure.payment.execution.RealPaymentExecutor;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Epic("Pagamentos")
@Feature("Pagamento Minimo")
@Story("Execucao real de pagamento minimo")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public final class PagamentoMinimoMassEffectfulRunnerIT {

    @Test(groups = {"effectful"})
    @Description(
            "Executa pagamento minimo real utilizando uma massa CSV, "
                    + "registra o pagamento e gera evidencias para o Allure."
    )
    public void executarPagamentoMinimo()
            throws Exception {

        String ambiente =
                propriedadeObrigatoria("env");

        String ambienteEsperado =
                System.getProperty(
                        "pagamento.minimo.mass.expected.env",
                        "128"
                );

        Assert.assertEquals(
                ambiente,
                ambienteEsperado,
                "Ambiente diferente do autorizado."
        );

        String modo =
                propriedadeObrigatoria(
                        "financial.mode"
                );

        Assert.assertEquals(
                modo.toUpperCase(),
                "REAL",
                "Use -Dfinancial.mode=REAL."
        );

        Path arquivo =
                Paths.get(
                        System.getProperty(
                                "pagamento.minimo.mass.file",
                                "src/test/resources/massas/"
                                        + "pagamento-minimo-demo-1.csv"
                        )
                );

        LocalDate dataPagamento =
                LocalDate.parse(
                        propriedadeObrigatoria(
                                "dataPagamento"
                        )
                );

        MassEntry massa =
                carregarMassa(
                        arquivo
                );

        registrarParametros(
                ambiente,
                arquivo,
                dataPagamento,
                massa
        );

        Allure.step(
                "1. Massa de pagamento minimo carregada"
        );

        EffectfulExecutionGuard
                .exigirAutorizacao();

        Allure.step(
                "2. Operacao com efeito autorizada"
        );

        AccountId accountId =
                new AccountId(
                        massa.idConta
                );

        FinancialScenarioPaymentMapper mapper =
                new FinancialScenarioPaymentMapper();

        PaymentInstruction instruction =
                new PaymentInstruction(
                        accountId,
                        mapper.map(
                                new MinimumPaymentScenario()
                        ),
                        massa.valorMinimo,
                        Instant.now()
                );

        Allure.step(
                "3. PaymentInstruction criada para pagamento minimo"
        );

        RealPaymentExecutor executor =
                new RealPaymentExecutor(
                        massa.idBoleto,
                        dataPagamento
                );

        Allure.step(
                "4. Executar pagamento real"
        );

        PaymentExecutionResult resultado =
                executor.execute(
                        instruction
                );

        anexarResultado(
                massa,
                dataPagamento,
                resultado
        );

        salvarCsv(
                massa,
                dataPagamento,
                resultado
        );

        Assert.assertTrue(
                resultado.isSuccessful(),
                resultado.getMessage()
        );
    }

    private static MassEntry carregarMassa(
            Path arquivo
    ) throws Exception {

        if (!Files.exists(arquivo)) {
            throw new IllegalArgumentException(
                    "Arquivo de massa nao encontrado: "
                            + arquivo.toAbsolutePath()
            );
        }

        List<String> linhas =
                Files.readAllLines(
                        arquivo,
                        StandardCharsets.UTF_8
                );

        if (linhas.size() < 2) {
            throw new IllegalArgumentException(
                    "A massa deve possuir cabecalho e uma conta."
            );
        }

        String cabecalho =
                linhas.get(0)
                        .replace("\uFEFF", "")
                        .trim();

        if (!"idConta;idBoleto;valorMinimo;ativo"
                .equalsIgnoreCase(cabecalho)) {

            throw new IllegalArgumentException(
                    "Cabecalho esperado: "
                            + "idConta;idBoleto;valorMinimo;ativo"
            );
        }

        String[] colunas =
                linhas.get(1)
                        .trim()
                        .split(";", -1);

        if (colunas.length != 4) {
            throw new IllegalArgumentException(
                    "Linha da massa invalida."
            );
        }

        String idConta =
                colunas[0].trim();

        long idBoleto =
                Long.parseLong(
                        colunas[1].trim()
                );

        BigDecimal valorMinimo =
                new BigDecimal(
                        colunas[2]
                                .trim()
                                .replace(',', '.')
                );

        boolean ativo =
                Boolean.parseBoolean(
                        colunas[3].trim()
                );

        if (idConta.isBlank()) {
            throw new IllegalArgumentException(
                    "IdConta obrigatorio."
            );
        }

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "IdBoleto deve ser maior que zero."
            );
        }

        if (valorMinimo.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Valor minimo deve ser maior que zero."
            );
        }

        if (!ativo) {
            throw new IllegalArgumentException(
                    "A conta da massa esta inativa."
            );
        }

        return new MassEntry(
                idConta,
                idBoleto,
                valorMinimo
        );
    }

    private static void registrarParametros(
            String ambiente,
            Path arquivo,
            LocalDate dataPagamento,
            MassEntry massa
    ) {

        Allure.parameter(
                "Ambiente",
                ambiente
        );

        Allure.parameter(
                "Cenario",
                "PAGAMENTO_MINIMO"
        );

        Allure.parameter(
                "IdConta",
                massa.idConta
        );

        Allure.parameter(
                "IdBoleto",
                massa.idBoleto
        );

        Allure.parameter(
                "Valor minimo",
                massa.valorMinimo
        );

        Allure.parameter(
                "Data pagamento",
                dataPagamento
        );

        Allure.parameter(
                "Arquivo de massa",
                arquivo.toString()
        );

        Allure.parameter(
                "Operacao com efeito",
                true
        );
    }

    private static void anexarResultado(
            MassEntry massa,
            LocalDate dataPagamento,
            PaymentExecutionResult resultado
    ) {

        String evidencia =
                "PAGAMENTO MINIMO - RESULTADO"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "IdConta: "
                        + massa.idConta
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + massa.idBoleto
                        + System.lineSeparator()
                        + "Valor minimo: "
                        + massa.valorMinimo
                        + System.lineSeparator()
                        + "Data pagamento: "
                        + dataPagamento
                        + System.lineSeparator()
                        + "IdEventoPagamento: "
                        + resultado.getExternalPaymentId()
                        + System.lineSeparator()
                        + "Executado em: "
                        + resultado.getExecutedAt()
                        + System.lineSeparator()
                        + "Sucesso: "
                        + resultado.isSuccessful()
                        + System.lineSeparator()
                        + "Mensagem: "
                        + resultado.getMessage();

        Allure.addAttachment(
                "Resumo executivo - Pagamento Minimo",
                "text/plain",
                evidencia
        );

        Allure.step(
                "5. Pagamento registrado - IdEventoPagamento: "
                        + resultado.getExternalPaymentId()
        );
    }

    private static void salvarCsv(
            MassEntry massa,
            LocalDate dataPagamento,
            PaymentExecutionResult resultado
    ) throws Exception {

        Path diretorio =
                Paths.get(
                        "target",
                        "pagamento-minimo-results"
                );

        Files.createDirectories(
                diretorio
        );

        String timestamp =
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        );

        Path arquivo =
                diretorio.resolve(
                        "pagamento-minimo-real-"
                                + timestamp
                                + ".csv"
                );

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                arquivo,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    "idConta;idBoleto;cenario;"
                            + "valorMinimo;dataPagamento;"
                            + "idEventoPagamento;status;"
                            + "executadoEm;mensagem"
            );

            writer.newLine();

            writer.write(
                    massa.idConta
                            + ";"
                            + massa.idBoleto
                            + ";MINIMUM;"
                            + massa.valorMinimo
                                    .toPlainString()
                            + ";"
                            + dataPagamento
                            + ";"
                            + limpar(
                                    resultado
                                            .getExternalPaymentId()
                            )
                            + ";"
                            + (
                                    resultado.isSuccessful()
                                            ? "SUCCESS"
                                            : "FAILED"
                            )
                            + ";"
                            + resultado.getExecutedAt()
                            + ";"
                            + limpar(
                                    resultado.getMessage()
                            )
            );
        }

        Allure.addAttachment(
                "Resultado CSV - Pagamento Minimo",
                "text/csv",
                Files.readString(
                        arquivo,
                        StandardCharsets.UTF_8
                )
        );

        System.out.println();
        System.out.println(
                "=========================================="
        );
        System.out.println(
                " PAGAMENTO MINIMO - RESULTADO"
        );
        System.out.println(
                "=========================================="
        );
        System.out.println(
                "Conta.................: "
                        + massa.idConta
        );
        System.out.println(
                "Boleto................: "
                        + massa.idBoleto
        );
        System.out.println(
                "Valor minimo...........: "
                        + massa.valorMinimo
        );
        System.out.println(
                "IdEventoPagamento......: "
                        + resultado.getExternalPaymentId()
        );
        System.out.println(
                "Status.................: "
                        + (
                                resultado.isSuccessful()
                                        ? "SUCCESS"
                                        : "FAILED"
                        )
        );
        System.out.println(
                "CSV....................: "
                        + arquivo.toAbsolutePath()
        );
        System.out.println(
                "=========================================="
        );
    }

    private static String propriedadeObrigatoria(
            String nome
    ) {

        String valor =
                System.getProperty(
                        nome
                );

        if (valor == null
                || valor.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Informe -D"
                            + nome
                            + "=..."
            );
        }

        return valor.trim();
    }

    private static String limpar(
            String valor
    ) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace(';', ',')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static final class MassEntry {

        private final String idConta;
        private final long idBoleto;
        private final BigDecimal valorMinimo;

        private MassEntry(
                String idConta,
                long idBoleto,
                BigDecimal valorMinimo
        ) {

            this.idConta =
                    idConta;

            this.idBoleto =
                    idBoleto;

            this.valorMinimo =
                    valorMinimo;
        }
    }
}
