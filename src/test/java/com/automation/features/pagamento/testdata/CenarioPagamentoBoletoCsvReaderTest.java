package com.automation.features.pagamento.testdata;

import com.automation.features.pagamento.model.ModalidadePagamento;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class CenarioPagamentoBoletoCsvReaderTest {

    private static final String ARQUIVO_DE_TESTE =
            "testdata/pagamento/"
                    + "cenarios-pagamento-test.csv";

    @Test(
            groups = {
                    "unit",
                    "pagamento",
                    "testdata"
            }
    )
    public void deveCarregarCenariosDePagamentoDoCsv() {

        CenarioPagamentoBoletoCsvReader reader =
                new CenarioPagamentoBoletoCsvReader(
                        ARQUIVO_DE_TESTE
                );

        List<CenarioPagamentoBoleto> cenarios =
                reader.carregar();

        Assert.assertNotNull(
                cenarios,
                "A lista de cenários não pode ser nula."
        );

        Assert.assertEquals(
                cenarios.size(),
                2,
                "O CSV de teste deve possuir dois cenários."
        );

        CenarioPagamentoBoleto cenarioAtivo =
                cenarios.get(0);

        Assert.assertEquals(
                cenarioAtivo.getNome(),
                "Pagamento teste ativo",
                "O nome do primeiro cenário está incorreto."
        );

        Assert.assertEquals(
                cenarioAtivo.getIdBoleto(),
                123456L,
                "O IdBoleto do primeiro cenário está incorreto."
        );

        Assert.assertEquals(
                cenarioAtivo.getModalidade(),
                ModalidadePagamento.TOTAL,
                "A modalidade do primeiro cenário está incorreta."
        );

        Assert.assertTrue(
                cenarioAtivo.isAtivo(),
                "O primeiro cenário deveria estar ativo."
        );

        CenarioPagamentoBoleto cenarioInativo =
                cenarios.get(1);

        Assert.assertEquals(
                cenarioInativo.getNome(),
                "Pagamento teste inativo",
                "O nome do segundo cenário está incorreto."
        );

        Assert.assertEquals(
                cenarioInativo.getIdBoleto(),
                654321L,
                "O IdBoleto do segundo cenário está incorreto."
        );

        Assert.assertEquals(
                cenarioInativo.getModalidade(),
                ModalidadePagamento.TOTAL,
                "A modalidade do segundo cenário está incorreta."
        );

        Assert.assertFalse(
                cenarioInativo.isAtivo(),
                "O segundo cenário deveria estar inativo."
        );
    }
}