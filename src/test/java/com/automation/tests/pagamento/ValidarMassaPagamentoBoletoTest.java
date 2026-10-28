package com.automation.tests.pagamento;

import com.automation.features.pagamento.model
        .BoletoParaPagamento;

import com.automation.features.pagamento.model
        .ModalidadePagamento;

import com.automation.features.pagamento.model
        .PagamentoBoleto;

import com.automation.features.pagamento.model
        .PagamentoExistente;

import com.automation.features.pagamento.repository
        .BoletoParaPagamentoRepository;

import com.automation.features.pagamento.repository
        .PagamentoDuplicidadeRepository;

import com.automation.features.pagamento.testdata
        .CenarioPagamentoBoleto;

import com.automation.features.pagamento.testdata
        .PagamentoBoletoDataProvider;

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
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Epic("Pagamentos")
@Feature("Pagamento de boletos")
@Story("Pré-validação da massa")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class ValidarMassaPagamentoBoletoTest {

    private static final String PROPRIEDADE_DATA_PAGAMENTO =
            "dataPagamento";

    @Test(
            dataProvider =
                    PagamentoBoletoDataProvider
                            .CENARIOS_PAGAMENTO,
            dataProviderClass =
                    PagamentoBoletoDataProvider.class,
            groups = {
                    "integration",
                    "database",
                    "pagamento",
                    "mass",
                    "readonly"
            }
    )
    @Description(
            "Valida os boletos ativos da massa antes de qualquer "
                    + "inserção, verificando existência, dados "
                    + "obrigatórios e possíveis duplicidades."
    )
    public void deveValidarBoletoAntesDoPagamento(
            CenarioPagamentoBoleto cenario
    ) throws Exception {

        LocalDate dataPagamento =
                obterDataPagamento();

        registrarParametros(
                cenario,
                dataPagamento
        );

        validarModalidade(
                cenario
        );

        BoletoParaPagamentoRepository boletoRepository =
                new BoletoParaPagamentoRepository();

        PagamentoDuplicidadeRepository duplicidadeRepository =
                new PagamentoDuplicidadeRepository();

        BoletoParaPagamento boleto =
                consultarBoleto(
                        boletoRepository,
                        cenario.getIdBoleto()
                );

        validarBoleto(
                boleto,
                cenario
        );

        PagamentoBoleto pagamento =
                criarPagamento(
                        boleto,
                        dataPagamento
                );

        validarAusenciaDeDuplicidade(
                duplicidadeRepository,
                pagamento
        );

        anexarResultado(
                cenario,
                boleto,
                pagamento
        );
    }

    @Step("Validar modalidade informada na massa")
    private void validarModalidade(
            CenarioPagamentoBoleto cenario
    ) {

        Assert.assertEquals(
                cenario.getModalidade(),
                ModalidadePagamento.TOTAL,
                "Atualmente somente a modalidade TOTAL "
                        + "é suportada."
        );
    }

    @Step("Consultar boleto da pré-validação")
    private BoletoParaPagamento consultarBoleto(
            BoletoParaPagamentoRepository boletoRepository,
            long idBoleto
    ) throws Exception {

        Optional<BoletoParaPagamento> resultado =
                boletoRepository.buscarPorIdBoleto(
                        idBoleto
                );

        Assert.assertTrue(
                resultado.isPresent(),
                "Nenhum boleto foi encontrado para o IdBoleto "
                        + idBoleto
                        + "."
        );

        return resultado.get();
    }

    @Step("Validar dados obrigatórios do boleto")
    private void validarBoleto(
            BoletoParaPagamento boleto,
            CenarioPagamentoBoleto cenario
    ) {

        Assert.assertEquals(
                boleto.getIdBoleto(),
                cenario.getIdBoleto(),
                "O IdBoleto retornado é diferente "
                        + "do informado na massa."
        );

        Assert.assertTrue(
                boleto.getIdConta() > 0,
                "O IdConta deve ser maior que zero."
        );

        Assert.assertNotNull(
                boleto.getTipoBoleto(),
                "O TipoBoleto não foi retornado."
        );

        Assert.assertNotNull(
                boleto.getNossoNumero(),
                "O NossoNumero não foi retornado."
        );

        Assert.assertFalse(
                boleto.getNossoNumero().isBlank(),
                "O NossoNumero foi retornado vazio."
        );

        Assert.assertNotNull(
                boleto.getValorBoleto(),
                "O ValorBoleto não foi retornado."
        );

        Assert.assertTrue(
                boleto.getValorBoleto().signum() > 0,
                "O ValorBoleto deve ser maior que zero."
        );
    }

    @Step("Criar pagamento para pré-validação")
    private PagamentoBoleto criarPagamento(
            BoletoParaPagamento boleto,
            LocalDate dataPagamento
    ) {

        return PagamentoBoleto.total(
                boleto,
                dataPagamento
        );
    }

    @Step("Validar ausência de pagamento duplicado")
    private void validarAusenciaDeDuplicidade(
            PagamentoDuplicidadeRepository duplicidadeRepository,
            PagamentoBoleto pagamento
    ) throws Exception {

        Optional<PagamentoExistente> pagamentoExistente =
                duplicidadeRepository
                        .buscarPagamentoExistente(
                                pagamento
                        );

        if (pagamentoExistente.isEmpty()) {
            return;
        }

        PagamentoExistente existente =
                pagamentoExistente.get();

        Assert.fail(
                "O boleto já possui um pagamento correspondente. "
                        + "IdEventoPagamento: "
                        + existente.getIdEventoPagamento()
                        + ". Status: "
                        + existente.getStatus()
                        + " - "
                        + existente.getDescricaoStatus()
                        + "."
        );
    }

    private LocalDate obterDataPagamento() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_DATA_PAGAMENTO
                );

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe a data planejada dos pagamentos com "
                            + "-DdataPagamento=AAAA-MM-DD."
            );
        }

        try {
            return LocalDate.parse(
                    valor.trim()
            );

        } catch (DateTimeParseException exception) {

            throw new IllegalArgumentException(
                    "A propriedade -DdataPagamento deve estar "
                            + "no formato AAAA-MM-DD. Valor informado: "
                            + valor,
                    exception
            );
        }
    }

    @Step("Registrar parâmetros da pré-validação")
    private void registrarParametros(
            CenarioPagamentoBoleto cenario,
            LocalDate dataPagamento
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
                "idBoleto",
                cenario.getIdBoleto()
        );

        Allure.parameter(
                "modalidade",
                cenario.getModalidade()
        );

        Allure.parameter(
                "dataPagamentoPlanejada",
                dataPagamento
        );

        Allure.parameter(
                "tipoExecucao",
                "Somente leitura"
        );
    }

    @Step("Anexar resultado da pré-validação")
    private void anexarResultado(
            CenarioPagamentoBoleto cenario,
            BoletoParaPagamento boleto,
            PagamentoBoleto pagamento
    ) {

        String resultado =
                "PRÉ-VALIDAÇÃO DO PAGAMENTO"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Cenario: "
                        + cenario.getNome()
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + boleto.getIdBoleto()
                        + System.lineSeparator()
                        + "TipoBoleto: "
                        + boleto.getTipoBoleto()
                        + System.lineSeparator()
                        + "IdConta: "
                        + boleto.getIdConta()
                        + System.lineSeparator()
                        + "NossoNumero: "
                        + boleto.getNossoNumero()
                        + System.lineSeparator()
                        + "ValorPagamento: "
                        + pagamento.getValorPagamento()
                        + System.lineSeparator()
                        + "DataPagamento: "
                        + pagamento.getDataPagamento()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Resultado: boleto elegível "
                        + "para pagamento.";

        Allure.addAttachment(
                "Resultado da pré-validação",
                "text/plain",
                resultado
        );
    }
}