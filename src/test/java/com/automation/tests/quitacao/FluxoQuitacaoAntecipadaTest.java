package com.automation.tests.quitacao;

import com.automation.core.config.ConfigReader;
import com.automation.core.network.EndpointAvailability;
import com.automation.core.network.EndpointAvailabilityChecker;
import com.automation.core.execution
        .EffectfulExecutionGuard;

import com.automation.features.quitacao.soap.QuitacaoSoapConfig;

import com.automation.features.quitacao.service
        .FluxoQuitacaoAntecipadaService;
import com.automation.features.pagamento.model
        .BoletoParaPagamento;

import com.automation.features.pagamento.model
        .TipoBoleto;

import com.automation.features.pagamento.repository
        .BoletoParaPagamentoRepository;

import com.automation.features.quitacao.model
        .BoletoEmitido;

import com.automation.features.quitacao.model
        .FluxoQuitacaoAntecipadaResultado;

import com.automation.features.quitacao.testdata
        .CenarioQuitacao;

import com.automation.features.quitacao.testdata
        .QuitacaoDataProvider;

import com.automation.features.quitacao.validator
        .BoletoEmitidoValidator;

import com.automation.features.quitacao.wait
        .BoletoEmitidoAwaiter;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

import org.testng.Assert;
import org.testng.SkipException;

import org.testng.annotations.Test;

import java.math.BigDecimal;

import java.util.Optional;
@Epic("Financial Validation Platform")
@Feature("Quitacao antecipada")
@Story("Fluxo integrado SOAP e banco de dados")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class FluxoQuitacaoAntecipadaTest {

    @Test(
            dataProvider =
                    QuitacaoDataProvider.CENARIOS_QUITACAO,
            dataProviderClass =
                    QuitacaoDataProvider.class,
            groups = {
                    "e2e",
                    "soap",
                    "database",
                    "quitacao",
                    "effectful"
            }
    )
    @Description(
            "Executa a simulação da quitação, solicita a geração "
                    + "do boleto e confirma sua persistência "
                    + "na tabela BoletosEmitidos."
    )
    public void deveExecutarFluxoCompletoDeQuitacaoAntecipada(
            CenarioQuitacao cenario
    ) throws Exception {

        definirNomeDoTeste(
                cenario
        );

        registrarParametrosDoTeste(
                cenario
        );

        validarDisponibilidadeDoAmbiente(cenario);

        EffectfulExecutionGuard
                .exigirAutorizacao();

        FluxoQuitacaoAntecipadaService fluxoService =
                new FluxoQuitacaoAntecipadaService();

        BoletoEmitidoAwaiter boletoAwaiter =
                new BoletoEmitidoAwaiter();

        BoletoEmitidoValidator boletoValidator =
                new BoletoEmitidoValidator();

        long idConta =
                cenario.getIdConta();

        String tipoCorrespondencia =
                cenario.getTipoCorrespondencia();

        FluxoQuitacaoAntecipadaResultado resultadoFluxo =
                fluxoService.executar(
                        idConta,
                        tipoCorrespondencia
                );

        long idBoleto =
                resultadoFluxo.getIdBoleto();

        Allure.parameter(
                "idBoleto",
                idBoleto
        );

        Optional<BoletoEmitido> boletoEmitido =
                boletoAwaiter.aguardar(
                        idConta,
                        idBoleto
                );

        boletoValidator.validar(
                boletoEmitido,
                idConta,
                idBoleto
        );
        BoletoParaPagamento boletoFinanceiro =
                new BoletoParaPagamentoRepository()
                        .buscarPorIdBoleto(
                                idBoleto
                        )
                        .orElseThrow(
                                () -> new AssertionError(
                                        "Boleto "
                                                + idBoleto
                                                + " foi gerado pelo SOAP, "
                                                + "mas nao foi localizado "
                                                + "para validacao financeira."
                                )
                        );

        validarFinanceiramenteBoletoGerado(
                resultadoFluxo,
                boletoFinanceiro,
                idConta,
                idBoleto
        );

        anexarResumoExecutivo(
                cenario,
                resultadoFluxo
        );
    }

        @Step("Validar valor financeiro da quitacao entre SOAP e banco")
    private void validarFinanceiramenteBoletoGerado(
            FluxoQuitacaoAntecipadaResultado resultadoFluxo,
            BoletoParaPagamento boleto,
            long idContaEsperado,
            long idBoletoEsperado
    ) {

        BigDecimal totalAntecipacao =
                resultadoFluxo
                        .getRespostaSimulacao()
                        .getTotalAntecipacao();

        BigDecimal valorBoleto =
                boleto.getValorBoleto();

        Assert.assertEquals(
                boleto.getIdBoleto(),
                idBoletoEsperado,
                "IdBoleto financeiro diferente do retornado pelo SOAP."
        );

        Assert.assertEquals(
                boleto.getIdConta(),
                idContaEsperado,
                "IdConta do boleto diferente da conta simulada."
        );

        Assert.assertEquals(
                boleto.getTipoBoleto(),
                TipoBoleto.QUITACAO,
                "O boleto gerado nao possui tipo QUITACAO."
        );

        Assert.assertNotNull(
                totalAntecipacao,
                "TotalAntecipacao nao foi retornado pela simulacao."
        );

        Assert.assertNotNull(
                valorBoleto,
                "ValorBoleto nao foi retornado pelo banco."
        );

        Assert.assertTrue(
                totalAntecipacao.signum() > 0,
                "TotalAntecipacao deve ser maior que zero."
        );

        Assert.assertTrue(
                valorBoleto.signum() > 0,
                "ValorBoleto deve ser maior que zero."
        );

        Assert.assertEquals(
                valorBoleto.compareTo(totalAntecipacao),
                0,
                "ValorBoleto SQL difere do TotalAntecipacao SOAP."
        );

        Assert.assertNotNull(
                boleto.getNossoNumero(),
                "NossoNumero nao foi retornado."
        );

        Assert.assertFalse(
                boleto.getNossoNumero().isBlank(),
                "NossoNumero foi retornado vazio."
        );

        BigDecimal diferenca =
                valorBoleto.subtract(
                        totalAntecipacao
                );

        Allure.parameter(
                "TotalAntecipacao SOAP",
                totalAntecipacao
        );

        Allure.parameter(
                "ValorBoleto SQL",
                valorBoleto
        );

        Allure.parameter(
                "NossoNumero",
                boleto.getNossoNumero()
        );

        Allure.parameter(
                "Diferenca SOAP x SQL",
                diferenca
        );

        String evidencia =
                "VALIDACAO FINANCEIRA DA QUITACAO"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "IdConta: "
                        + boleto.getIdConta()
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + boleto.getIdBoleto()
                        + System.lineSeparator()
                        + "TipoBoleto: "
                        + boleto.getTipoBoleto()
                        + System.lineSeparator()
                        + "NossoNumero: "
                        + boleto.getNossoNumero()
                        + System.lineSeparator()
                        + "TotalAntecipacao SOAP: "
                        + totalAntecipacao
                        + System.lineSeparator()
                        + "ValorBoleto SQL: "
                        + valorBoleto
                        + System.lineSeparator()
                        + "Diferenca: "
                        + diferenca
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Resultado: PASS";

        Allure.addAttachment(
                "Validacao financeira SOAP x SQL",
                "text/plain",
                evidencia
        );

        System.out.println();
        System.out.println(
                "================ SETTLEMENT FINANCIAL VALIDATION ================"
        );

        System.out.println(
                "Conta.....................: "
                        + boleto.getIdConta()
        );

        System.out.println(
                "IdBoleto..................: "
                        + boleto.getIdBoleto()
        );

        System.out.println(
                "TipoBoleto................: "
                        + boleto.getTipoBoleto()
        );

        System.out.println(
                "NossoNumero...............: "
                        + boleto.getNossoNumero()
        );

        System.out.println(
                "TotalAntecipacao SOAP.....: "
                        + totalAntecipacao
        );

        System.out.println(
                "ValorBoleto SQL...........: "
                        + valorBoleto
        );

        System.out.println(
                "Diferenca.................: "
                        + diferenca
        );

        System.out.println(
                "SETTLEMENT VALIDATION.....: PASS"
        );

        System.out.println(
                "================================================================="
        );
    }


    @Step("Verificar disponibilidade do ambiente SOAP")
    private void validarDisponibilidadeDoAmbiente(
            CenarioQuitacao cenario
    ) {

        String wsUrl =
                ConfigReader.getProperty(
                        "WS_URL"
                );

        String endpoint =
                wsUrl.endsWith("/")
                        ? wsUrl
                                + QuitacaoSoapConfig.ENDPOINT_CONTA
                        : wsUrl
                                + "/"
                                + QuitacaoSoapConfig.ENDPOINT_CONTA;

        int timeoutSeconds =
                Integer.parseInt(
                        System.getProperty(
                                "environment.preflight.timeoutSeconds",
                                "3"
                        )
                );

        EndpointAvailability availability =
                new EndpointAvailabilityChecker()
                        .check(
                                endpoint,
                                timeoutSeconds * 1000
                        );

        Allure.parameter(
                "soapEndpoint",
                endpoint
        );

        Allure.parameter(
                "environmentStatus",
                availability.isAvailable()
                        ? "AVAILABLE"
                        : "UNAVAILABLE"
        );

        String evidencia =
                "ENVIRONMENT PREFLIGHT"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Endpoint: "
                        + availability.getEndpoint()
                        + System.lineSeparator()
                        + "Host: "
                        + availability.getHost()
                        + System.lineSeparator()
                        + "Port: "
                        + availability.getPort()
                        + System.lineSeparator()
                        + "ResolvedAddress: "
                        + availability.getResolvedAddress()
                        + System.lineSeparator()
                        + "Available: "
                        + availability.isAvailable()
                        + System.lineSeparator()
                        + "Detail: "
                        + availability.getDetail()
                        + System.lineSeparator()
                        + "ElapsedMillis: "
                        + availability.getElapsedMillis();

        Allure.addAttachment(
                "Environment preflight",
                "text/plain",
                evidencia
        );

        System.out.println();
        System.out.println(
                "================ ENVIRONMENT PREFLIGHT ================="
        );

        System.out.println(
                "Endpoint..................: "
                        + availability.getEndpoint()
        );

        System.out.println(
                "Resolved address..........: "
                        + availability.getResolvedAddress()
        );

        System.out.println(
                "Port......................: "
                        + availability.getPort()
        );

        System.out.println(
                "Status....................: "
                        + (
                        availability.isAvailable()
                                ? "AVAILABLE"
                                : "UNAVAILABLE"
                )
        );

        System.out.println(
                "Detail....................: "
                        + availability.getDetail()
        );

        System.out.println(
                "========================================================="
        );

        if (!availability.isAvailable()) {

            anexarResumoExecutivoAmbienteIndisponivel(
                    cenario,
                    availability
            );

            throw new SkipException(
                    "ENVIRONMENT UNAVAILABLE | "
                            + availability.getEndpoint()
                            + " | "
                            + availability.getDetail()
            );
        }
    }
    @Step("Anexar resumo executivo de ambiente indisponivel")
    private void anexarResumoExecutivoAmbienteIndisponivel(
            CenarioQuitacao cenario,
            EndpointAvailability availability
    ) {

        String quebra =
                System.lineSeparator();

        String resumo =
                "FINANCIAL VALIDATION PLATFORM"
                        + quebra
                        + "========================================"
                        + quebra
                        + "RESUMO EXECUTIVO - QUITACAO ANTECIPADA"
                        + quebra
                        + "========================================"
                        + quebra
                        + quebra
                        + "Ambiente..................: "
                        + System.getProperty(
                                "env",
                                "NAO INFORMADO"
                        )
                        + quebra
                        + "Conta.....................: "
                        + cenario.getIdConta()
                        + quebra
                        + quebra
                        + "Preflight.................: UNAVAILABLE"
                        + quebra
                        + "Endpoint..................: "
                        + availability.getEndpoint()
                        + quebra
                        + "IP resolvido..............: "
                        + availability.getResolvedAddress()
                        + quebra
                        + "Porta.....................: "
                        + availability.getPort()
                        + quebra
                        + "Motivo....................: "
                        + availability.getDetail()
                        + quebra
                        + quebra
                        + "Simulacao SOAP............: NAO EXECUTADA"
                        + quebra
                        + "Geracao do boleto.........: NAO EXECUTADA"
                        + quebra
                        + "Persistencia SQL..........: NAO EXECUTADA"
                        + quebra
                        + "Validacao SOAP x SQL......: NAO EXECUTADA"
                        + quebra
                        + quebra
                        + "Resultado funcional.......: NAO AVALIADO"
                        + quebra
                        + "Classificacao.............: ENVIRONMENT UNAVAILABLE";

        Allure.addAttachment(
                "Resumo executivo - Quitacao antecipada",
                "text/plain",
                resumo
        );
    }

private void definirNomeDoTeste(
            CenarioQuitacao cenario
    ) {

        Allure.getLifecycle()
                .updateTestCase(
                        testResult ->
                                testResult.setName(
                                        "Quitação antecipada - "
                                                + cenario.getNome()
                                                + " [Conta "
                                                + cenario.getIdConta()
                                                + "]"
                                )
                );
    }

    @Step("Registrar parâmetros do fluxo")
    private void registrarParametrosDoTeste(
            CenarioQuitacao cenario
    ) {

        Allure.parameter(
                "ambiente",
                System.getProperty(
                        "env",
                        "não informado"
                )
        );

        Allure.parameter(
                "cenario",
                cenario.getNome()
        );

        Allure.parameter(
                "idConta",
                cenario.getIdConta()
        );

        Allure.parameter(
                "tipoCorrespondencia",
                cenario.getTipoCorrespondencia()
        );

        Allure.parameter(
                "operacaoComEfeito",
                true
        );
    }

    @Step("Anexar resumo executivo do fluxo de quitação")
    private void anexarResumoExecutivo(
            CenarioQuitacao cenario,
            FluxoQuitacaoAntecipadaResultado resultadoFluxo
    ) {

        String resumo =
                "FLUXO COMPLETO DE QUITAÇÃO ANTECIPADA"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Cenário: "
                        + cenario.getNome()
                        + System.lineSeparator()
                        + "Ambiente: "
                        + System.getProperty(
                                "env",
                                "não informado"
                        )
                        + System.lineSeparator()
                        + "IdConta: "
                        + cenario.getIdConta()
                        + System.lineSeparator()
                        + "TipoCorrespondencia: "
                        + cenario.getTipoCorrespondencia()
                        + System.lineSeparator()
                        + "TotalAntecipacao: "
                        + resultadoFluxo
                                .getRespostaSimulacao()
                                .getTotalAntecipacao()
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + resultadoFluxo
                                .getRespostaGeracao()
                                .getIdBoleto()
                        + System.lineSeparator()
                        + "IDLog: "
                        + resultadoFluxo
                                .getRespostaGeracao()
                                .getIdLog()
                        + System.lineSeparator()
                        + "BoletoRegistrado: "
                        + resultadoFluxo
                                .getRespostaGeracao()
                                .isRegistrado()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Resultado: boleto gerado e localizado "
                        + "com sucesso na base de dados.";

        Allure.addAttachment(
                "Resumo executivo da quitação",
                "text/plain",
                resumo
        );
    }
}