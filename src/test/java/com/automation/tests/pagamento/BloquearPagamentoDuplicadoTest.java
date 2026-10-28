package com.automation.tests.pagamento;

import com.automation.features.pagamento.model
        .BoletoParaPagamento;

import com.automation.features.pagamento.model
        .PagamentoBoleto;

import com.automation.features.pagamento.repository
        .BoletoParaPagamentoRepository;

import com.automation.features.pagamento.service
        .PagamentoBoletoService;

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
@Story("Proteção contra pagamento duplicado")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class BloquearPagamentoDuplicadoTest {

    private static final String PROPRIEDADE_ID_BOLETO =
            "idBoleto";

    private static final String PROPRIEDADE_DATA_PAGAMENTO =
            "dataPagamento";

    private static final String MENSAGEM_DUPLICIDADE =
            "já existe um registro correspondente";

    @Test(
            groups = {
                    "integration",
                    "database",
                    "pagamento",
                    "duplicate",
                    "readonly"
            }
    )
    @Description(
            "Tenta inserir novamente um pagamento existente "
                    + "e valida que a proteção contra duplicidade "
                    + "impede a criação de um novo registro."
    )
    public void deveBloquearPagamentoDuplicado()
            throws Exception {

        long idBoleto =
                obterIdBoleto();

        LocalDate dataPagamento =
                obterDataPagamento();

        registrarParametros(
                idBoleto,
                dataPagamento
        );

        BoletoParaPagamentoRepository boletoRepository =
                new BoletoParaPagamentoRepository();

        PagamentoBoletoService pagamentoService =
                new PagamentoBoletoService();

        BoletoParaPagamento boleto =
                consultarBoleto(
                        boletoRepository,
                        idBoleto
                );

        PagamentoBoleto pagamento =
                PagamentoBoleto.total(
                        boleto,
                        dataPagamento
                );

        validarBloqueio(
                pagamentoService,
                pagamento
        );
    }

    @Step("Consultar boleto que já possui pagamento")
    private BoletoParaPagamento consultarBoleto(
            BoletoParaPagamentoRepository boletoRepository,
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

    @Step("Validar bloqueio do pagamento duplicado")
    private void validarBloqueio(
            PagamentoBoletoService pagamentoService,
            PagamentoBoleto pagamento
    ) throws Exception {

        try {
            pagamentoService.inserir(
                    pagamento
            );

            Assert.fail(
                    "O pagamento duplicado não foi bloqueado."
            );

        } catch (IllegalStateException exception) {

            String mensagem =
                    exception.getMessage();

            Allure.addAttachment(
                    "Resultado da proteção contra duplicidade",
                    "text/plain",
                    mensagem == null
                            ? "Exceção sem mensagem."
                            : mensagem
            );

            Assert.assertNotNull(
                    mensagem,
                    "A exceção de duplicidade não apresentou mensagem."
            );

            Assert.assertTrue(
                    mensagem.contains(
                            MENSAGEM_DUPLICIDADE
                    ),
                    "A operação foi bloqueada por outro motivo. "
                            + "Mensagem recebida: "
                            + mensagem
            );
        }
    }

    private long obterIdBoleto() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_ID_BOLETO
                );

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe um boleto que já possui pagamento "
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
                    "Informe a data do pagamento existente "
                            + "utilizando -DdataPagamento=AAAA-MM-DD."
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

    @Step("Registrar parâmetros do teste de duplicidade")
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
                "resultadoEsperado",
                "Pagamento duplicado bloqueado"
        );

        Allure.parameter(
                "tipoExecucao",
                "Somente leitura"
        );
    }
}