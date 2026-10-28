package com.automation.features.quitacao.testdata;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class CenarioQuitacaoCsvReaderTest {

    @Test
    public void deveCarregarCenariosDeQuitacaoDoCsv() {

        CenarioQuitacaoCsvReader reader =
                new CenarioQuitacaoCsvReader();

        List<CenarioQuitacao> cenarios =
                reader.carregar();

        Assert.assertNotNull(
                cenarios,
                "A lista de cenários não pode ser nula."
        );

        Assert.assertFalse(
                cenarios.isEmpty(),
                "O arquivo deve possuir pelo menos um cenário."
        );

        boolean possuiCenarioAtivo = false;

        for (CenarioQuitacao cenario : cenarios) {

            Assert.assertNotNull(
                    cenario,
                    "O CSV retornou um cenário nulo."
            );

            Assert.assertFalse(
                    cenario.getNome().isBlank(),
                    "O nome do cenário não pode ser vazio."
            );

            Assert.assertTrue(
                    cenario.getIdConta() > 0,
                    "O IdConta deve ser maior que zero."
            );

            Assert.assertFalse(
                    cenario
                            .getTipoCorrespondencia()
                            .isBlank(),
                    "O TipoCorrespondencia não pode ser vazio."
            );

            if (cenario.isAtivo()) {
                possuiCenarioAtivo = true;
            }
        }

        Assert.assertTrue(
                possuiCenarioAtivo,
                "O CSV deve possuir pelo menos "
                        + "um cenário ativo."
        );
    }
}
