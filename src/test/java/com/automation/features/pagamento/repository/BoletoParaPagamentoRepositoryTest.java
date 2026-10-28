package com.automation.features.pagamento.repository;

import com.automation.features.pagamento.model.BoletoParaPagamento;

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

import java.util.Optional;

@Epic("Pagamentos")
@Feature("Pagamento de boletos")
@Story("Consultar boleto para pagamento")
@Owner("QA Automation")
@Severity(SeverityLevel.CRITICAL)
public class BoletoParaPagamentoRepositoryTest {

    @Test(
            groups = {
                    "integration",
                    "database",
                    "pagamento",
                    "readonly"
            }
    )
    @Description(
            "Consulta um boleto pelo IdBoleto e valida os dados "
                    + "necessários para a futura inserção do pagamento."
    )
    public void deveConsultarBoletoParaPagamentoComSucesso()
            throws Exception {

        long idBoletoEsperado =
                obterIdBoletoInformado();

        Allure.parameter(
                "idBoleto",
                idBoletoEsperado
        );

        BoletoParaPagamentoRepository repository =
                new BoletoParaPagamentoRepository();

        Optional<BoletoParaPagamento> resultado =
                repository.buscarPorIdBoleto(
                        idBoletoEsperado
                );

        Assert.assertTrue(
                resultado.isPresent(),
                "O boleto "
                        + idBoletoEsperado
                        + " não foi encontrado."
        );

        BoletoParaPagamento boleto =
                resultado.get();

        Assert.assertEquals(
                boleto.getIdBoleto(),
                idBoletoEsperado,
                "O IdBoleto retornado é diferente do informado."
        );

        Assert.assertTrue(
                boleto.getIdConta() > 0,
                "O IdConta retornado é inválido."
        );

        Assert.assertNotNull(
                boleto.getTipoBoleto(),
                "O TipoBoleto não foi identificado."
        );

        Assert.assertTrue(
                boleto.getValorBoleto().signum() > 0,
                "O ValorBoleto deve ser maior que zero."
        );

        Assert.assertFalse(
                boleto.getNossoNumero().isBlank(),
                "O NossoNumero não pode estar vazio."
        );

        anexarResumo(boleto);
    }

    private long obterIdBoletoInformado() {

        String valor =
                System.getProperty(
                        "idBoleto"
                );

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Informe o boleto utilizando "
                            + "-DidBoleto=NUMERO."
            );
        }

        try {
            long idBoleto =
                    Long.parseLong(
                            valor.trim()
                    );

            if (idBoleto <= 0) {
                throw new IllegalArgumentException(
                        "O IdBoleto deve ser maior que zero."
                );
            }

            return idBoleto;

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "O valor informado em -DidBoleto "
                            + "não é numérico: "
                            + valor,
                    exception
            );
        }
    }

    private void anexarResumo(
            BoletoParaPagamento boleto
    ) {

        String resumo =
                "BOLETO LOCALIZADO PARA PAGAMENTO"
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + boleto.getIdBoleto()
                        + System.lineSeparator()
                        + "IdConta: "
                        + boleto.getIdConta()
                        + System.lineSeparator()
                        + "TipoBoleto: "
                        + boleto.getTipoBoleto()
                        + " ("
                        + boleto.getTipoBoleto().getCodigo()
                        + ")"
                        + System.lineSeparator()
                        + "NossoNumero: "
                        + boleto.getNossoNumero()
                        + System.lineSeparator()
                        + "ValorBoleto: "
                        + boleto.getValorBoleto();

        Allure.addAttachment(
                "Resumo do boleto para pagamento",
                "text/plain",
                resumo
        );
    }
}