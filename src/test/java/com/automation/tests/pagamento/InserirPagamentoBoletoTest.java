package com.automation.tests.pagamento;

import com.automation.features.pagamento.model.BoletoParaPagamento;
import com.automation.features.pagamento.model.EventoExternoPagamento;
import com.automation.features.pagamento.model.PagamentoBoleto;

import com.automation.features.pagamento.repository.BoletoParaPagamentoRepository;
import com.automation.features.pagamento.repository.EventoExternoPagamentoRepository;

import com.automation.features.pagamento.service.PagamentoBoletoService;

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
@Story("Inserção de pagamento total")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class InserirPagamentoBoletoTest {

    private static final String PROPRIEDADE_ID_BOLETO =
            "idBoleto";

    private static final String PROPRIEDADE_DATA_PAGAMENTO =
            "dataPagamento";

    private BoletoParaPagamentoRepository boletoRepository;

    private PagamentoBoletoService pagamentoService;

    private EventoExternoPagamentoRepository eventoRepository;

    private EventoExternoPagamentoValidator eventoValidator;

    @Test(
            groups = {
                    "integration",
                    "database",
                    "pagamento",
                    "effectful"
            }
    )
    @Description(
            "Consulta o boleto pelo IdBoleto, verifica se o "
                    + "pagamento já existe, insere o pagamento total "
                    + "em EventosExternosPagamentos e valida "
                    + "o registro criado na base de dados."
    )
    public void deveInserirPagamentoTotalDoBoleto()
            throws Exception {

        long idBoleto =
                obterIdBoleto();

        LocalDate dataPagamento =
                obterDataPagamento();

        registrarParametros(
                idBoleto,
                dataPagamento
        );

        inicializarDependencias();

        BoletoParaPagamento boleto =
                consultarBoleto(idBoleto);

        PagamentoBoleto pagamento =
                PagamentoBoleto.total(
                        boleto,
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

    @Step("Consultar boleto informado para pagamento")
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

    @Step("Consultar evento externo inserido")
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

    private long obterIdBoleto() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_ID_BOLETO
                );

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe o boleto que receberá o pagamento "
                            + "utilizando -DidBoleto=NUMERO."
            );
        }

        try {
            long idBoleto =
                    Long.parseLong(
                            valor.trim()
                    );

            if (idBoleto <= 0) {
                throw new IllegalArgumentException(
                        "A propriedade -DidBoleto deve ser "
                                + "maior que zero."
                );
            }

            return idBoleto;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "A propriedade -DidBoleto não contém "
                            + "um número válido: "
                            + valor,
                    exception
            );
        }
    }

    private LocalDate obterDataPagamento() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_DATA_PAGAMENTO
                );

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe a data do pagamento utilizando "
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

    @Step("Registrar parâmetros da execução do pagamento")
    private void registrarParametros(
            long idBoleto,
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
                "idBoleto",
                idBoleto
        );

        Allure.parameter(
                "dataPagamento",
                dataPagamento
        );

        Allure.parameter(
                "modalidadePagamento",
                "TOTAL"
        );

        Allure.parameter(
                "operacaoComEfeito",
                true
        );
    }

    @Step("Anexar resumo executivo do pagamento")
    private void anexarResumoExecutivo(
            BoletoParaPagamento boleto,
            PagamentoBoleto pagamento,
            EventoExternoPagamento evento
    ) {

        String resumo =
                "PAGAMENTO TOTAL DE BOLETO"
                        + System.lineSeparator()
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
                        + "ModalidadePagamento: "
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
                        + "IdAutorizacao: "
                        + evento.getIdAutorizacao()
                        + System.lineSeparator()
                        + "IdOperacao: "
                        + evento.getIdOperacao()
                        + System.lineSeparator()
                        + "Status: "
                        + evento.getStatus()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "Resultado: pagamento inserido e validado "
                        + "com sucesso em EventosExternosPagamentos.";

        Allure.addAttachment(
                "Resumo executivo do pagamento",
                "text/plain",
                resumo
        );
    }
}