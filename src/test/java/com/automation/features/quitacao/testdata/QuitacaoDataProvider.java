package com.automation.features.quitacao.testdata;

import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.List;

public final class QuitacaoDataProvider {

    public static final String CENARIOS_QUITACAO =
            "cenariosQuitacao";

    private static final String PROPRIEDADE_ID_CONTA =
            "idConta";

    private static final String PROPRIEDADE_EXECUCAO_EM_MASSA =
            "permitirExecucaoEmMassa";

    private QuitacaoDataProvider() {
        // Impede instanciação.
    }

    @DataProvider(
            name = CENARIOS_QUITACAO,
            parallel = false
    )
    public static Object[][] fornecerCenarios() {

        CenarioQuitacaoCsvReader reader =
                new CenarioQuitacaoCsvReader();

        List<CenarioQuitacao> cenariosAtivos =
                filtrarCenariosAtivos(
                        reader.carregar()
                );

        Long idContaFiltrado =
                obterIdContaFiltrado();

        if (idContaFiltrado != null) {

            cenariosAtivos =
                    filtrarPorIdConta(
                            cenariosAtivos,
                            idContaFiltrado
                    );
        }

        validarCenariosEncontrados(
                cenariosAtivos,
                idContaFiltrado
        );

        validarExecucaoEmMassa(
                cenariosAtivos,
                idContaFiltrado
        );

        return converterParaDataProvider(
                cenariosAtivos
        );
    }

    private static List<CenarioQuitacao> filtrarCenariosAtivos(
            List<CenarioQuitacao> cenarios
    ) {

        List<CenarioQuitacao> ativos =
                new ArrayList<>();

        for (CenarioQuitacao cenario : cenarios) {

            if (cenario.isAtivo()) {
                ativos.add(cenario);
            }
        }

        return ativos;
    }

    private static Long obterIdContaFiltrado() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_ID_CONTA
                );

        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            long idConta =
                    Long.parseLong(
                            valor.trim()
                    );

            if (idConta <= 0) {
                throw new IllegalArgumentException(
                        "A propriedade -DidConta "
                                + "deve ser maior que zero."
                );
            }

            return idConta;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "A propriedade -DidConta "
                            + "não contém um número válido: "
                            + valor,
                    exception
            );
        }
    }

    private static List<CenarioQuitacao> filtrarPorIdConta(
            List<CenarioQuitacao> cenarios,
            long idConta
    ) {

        List<CenarioQuitacao> filtrados =
                new ArrayList<>();

        for (CenarioQuitacao cenario : cenarios) {

            if (cenario.getIdConta() == idConta) {
                filtrados.add(cenario);
            }
        }

        return filtrados;
    }

    private static void validarCenariosEncontrados(
            List<CenarioQuitacao> cenarios,
            Long idContaFiltrado
    ) {

        if (!cenarios.isEmpty()) {
            return;
        }

        if (idContaFiltrado != null) {
            throw new IllegalStateException(
                    "Nenhum cenário ativo foi encontrado "
                            + "para o IdConta "
                            + idContaFiltrado
                            + "."
            );
        }

        throw new IllegalStateException(
                "Nenhum cenário ativo foi encontrado "
                        + "no arquivo de massa."
        );
    }

    private static void validarExecucaoEmMassa(
            List<CenarioQuitacao> cenarios,
            Long idContaFiltrado
    ) {

        if (idContaFiltrado != null
                || cenarios.size() == 1) {

            return;
        }

        boolean execucaoPermitida =
                obterPermissaoDeExecucaoEmMassa();

        if (!execucaoPermitida) {
            throw new IllegalStateException(
                    "Foram encontrados "
                            + cenarios.size()
                            + " cenários ativos. "
                            + "Como os testes geram boletos, "
                            + "a execução em massa precisa ser "
                            + "autorizada com "
                            + "-DpermitirExecucaoEmMassa=true. "
                            + "Para executar somente uma conta, "
                            + "utilize -DidConta=NUMERO."
            );
        }
    }

    private static boolean obterPermissaoDeExecucaoEmMassa() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_EXECUCAO_EM_MASSA,
                        "false"
                );

        if ("true".equalsIgnoreCase(valor)) {
            return true;
        }

        if ("false".equalsIgnoreCase(valor)) {
            return false;
        }

        throw new IllegalArgumentException(
                "A propriedade "
                        + "-DpermitirExecucaoEmMassa "
                        + "deve conter true ou false."
        );
    }

    private static Object[][] converterParaDataProvider(
            List<CenarioQuitacao> cenarios
    ) {

        Object[][] dados =
                new Object[cenarios.size()][1];

        for (int indice = 0;
             indice < cenarios.size();
             indice++) {

            dados[indice][0] =
                    cenarios.get(indice);
        }

        return dados;
    }
}
