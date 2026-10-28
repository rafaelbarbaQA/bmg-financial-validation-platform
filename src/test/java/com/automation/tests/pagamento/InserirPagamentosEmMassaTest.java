package com.automation.tests.pagamento;

import com.automation.features.pagamento.model.BoletoParaPagamento;
import com.automation.features.pagamento.model.EventoExternoPagamento;
import com.automation.features.pagamento.model.ModalidadePagamento;
import com.automation.features.pagamento.model.PagamentoBoleto;

import com.automation.features.pagamento.repository.BoletoParaPagamentoRepository;
import com.automation.features.pagamento.repository.EventoExternoPagamentoRepository;

import com.automation.features.pagamento.service.PagamentoBoletoService;

import com.automation.features.pagamento.testdata
        .CenarioPagamentoBoleto;

import com.automation.features.pagamento.testdata
        .PagamentoBoletoDataProvider;

import com.automation.features.pagamento.validator
        .EventoExternoPagamentoValidator;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Epic("Pagamentos")
@Feature("Pagamento de boletos")
@Story("Execução em massa por IdBoleto")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class InserirPagamentosEmMassaTest {

    private static final String PROPRIEDADE_DATA_PAGAMENTO =
            "dataPagamento";

    private BoletoParaPagamentoRepository boletoRepository;

    private PagamentoBoletoService pagamentoService;

    private EventoExternoPagamentoRepository eventoRepository;

    private EventoExternoPagamentoValidator eventoValidator;

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
                    "effectful"
            }
    )
    @Description(
            "Lê os IdBoletos ativos do CSV do ambiente, "
                    + "consulta os dados dos boletos, verifica "
                    + "duplicidades, insere os pagamentos e valida "
                    + "cada registro em EventosExternosPagamentos."
    )
    public void deveInserirPagamentosEmMassa(
            CenarioPagamentoBoleto cenario
    ) throws Exception {

        LocalDate dataPagamento =
                obterDataPagamento();

        registrarParametros(
                cenario,
                dataPagamento
        );

        validarModalidadeSuportada(
                cenario
        );

        inicializarDependencias();

        BoletoParaPagamento boleto =
                consultarBoleto(
                        cenario.getIdBoleto()
                );

        PagamentoBoleto pagamento =
                criarPagamento(
                        boleto,
                        cenario,
                        dataPagamento
                );

        long idEventoPagamento =
                pagamentoService.inserir(
                        pagamento
                );

        Allure.parameter(
                "idEventoPagamento",
                idEventoPagamento
        );

        EventoExternoPagamento eventoInserido =
                consultarEventoInserido(
                        idEventoPagamento
                );

        eventoValidator.validar(
                eventoInserido,
                pagamento,
                idEventoPagamento
        );

        anexarResumoExecutivo(
                cenario,
                boleto,
                pagamento,
                eventoInserido
        );
    }

    private void inicializarDependencias() {

        boletoRepository =
                new BoletoParaPagamentoRepository();

        pagamentoService =
                new PagamentoBoletoService();

        eventoRepository =
                new EventoExternoPagamentoRepository();

        eventoValidator =
                new EventoExternoPagamentoValidator();
    }

    @Step("Validar modalidade de pagamento da massa")
    private void validarModalidadeSuportada(
            CenarioPagamentoBoleto cenario
    ) {

        if (cenario.getModalidade()
                != ModalidadePagamento.TOTAL) {

            throw new IllegalStateException(
                    "A modalidade "
                            + cenario.getModalidade()
                            + " ainda não é suportada. "
                            + "Atualmente somente TOTAL "
                            + "pode ser utilizada."
            );
        }
    }

    @Step("Consultar boleto informado na massa")
    private BoletoParaPagamento consultarBoleto(
            long idBoleto
    ) throws Exception {

        Optional<BoletoParaPagamento> resultado =
                boletoRepository.buscarPorIdBoleto(
                        idBoleto
                );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "Nenhum boleto foi encontrado para o IdBoleto "
                            + idBoleto
                            + "."
            );
        }

        return resultado.get();
    }

    @Step("Criar instrução de pagamento")
    private PagamentoBoleto criarPagamento(
            BoletoParaPagamento boleto,
            CenarioPagamentoBoleto cenario,
            LocalDate dataPagamento
    ) {

        if (cenario.getModalidade()
                == ModalidadePagamento.TOTAL) {

            return PagamentoBoleto.total(
                    boleto,
                    dataPagamento
            );
        }

        throw new IllegalStateException(
                "Não foi possível criar o pagamento "
                        + "para a modalidade "
                        + cenario.getModalidade()
                        + "."
        );
    }

    @Step("Consultar evento externo inserido pela massa")
    private EventoExternoPagamento consultarEventoInserido(
            long idEventoPagamento
    ) throws Exception {

        Optional<EventoExternoPagamento> resultado =
                eventoRepository
                        .buscarPorIdEventoPagamento(
                                idEventoPagamento
                        );

        if (resultado.isEmpty()) {
            throw new AssertionError(
                    "O pagamento foi inserido, mas o evento "
                            + idEventoPagamento
                            + " não foi localizado em "
                            + "EventosExternosPagamentos."
            );
        }

        return resultado.get();
    }

    private LocalDate obterDataPagamento() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_DATA_PAGAMENTO
                );

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe a data dos pagamentos utilizando "
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

    @Step("Registrar parâmetros do cenário de pagamento")
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
                "dataPagamento",
                dataPagamento
        );

        Allure.parameter(
                "operacaoComEfeito",
                true
        );
    }

    @Step("Anexar resumo executivo do pagamento da massa")
    private void anexarResumoExecutivo(
            CenarioPagamentoBoleto cenario,
            BoletoParaPagamento boleto,
            PagamentoBoleto pagamento,
            EventoExternoPagamento evento
    ) {

        String resumo =
                "PAGAMENTO DE BOLETO EM MASSA"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Cenario: "
                        + cenario.getNome()
                        + System.lineSeparator()
                        + "Ambiente: "
                        + System.getProperty(
                                "env",
                                "não informado"
                        )
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
                        + "ValorBoleto: "
                        + boleto.getValorBoleto()
                        + System.lineSeparator()
                        + "Modalidade: "
                        + pagamento.getModalidade()
                        + System.lineSeparator()
                        + "ValorPago: "
                        + pagamento.getValorPagamento()
                        + System.lineSeparator()
                        + "DataPagamento: "
                        + pagamento.getDataPagamento()
                        + System.lineSeparator()
                        + "IdEventoPagamento: "
                        + evento.getIdEventoPagamento()
                        + System.lineSeparator()
                        + "Status: "
                        + evento.getStatus()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Resultado: pagamento inserido e "
                        + "validado com sucesso.";

        Allure.addAttachment(
                "Resumo executivo do pagamento em massa",
                "text/plain",
                resumo
        );
    }
}