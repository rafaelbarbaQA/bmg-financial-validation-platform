package com.automation.features.pagamento.testdata;

import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.List;

public final class PagamentoBoletoDataProvider {

    public static final String CENARIOS_PAGAMENTO =
            "cenariosPagamentoBoleto";

    private static final String PROPRIEDADE_ID_BOLETO =
            "idBoleto";

    private static final String PROPRIEDADE_EXECUCAO_EM_MASSA =
            "permitirExecucaoEmMassa";

    private PagamentoBoletoDataProvider() {
        // Impede instanciação.
    }

    @DataProvider(
            name = CENARIOS_PAGAMENTO,
            parallel = false
    )
    public static Object[][] fornecerCenarios() {

        CenarioPagamentoBoletoCsvReader reader =
                new CenarioPagamentoBoletoCsvReader();

        List<CenarioPagamentoBoleto> cenariosAtivos =
                filtrarCenariosAtivos(
                        reader.carregar()
                );

        Long idBoletoFiltrado =
                obterIdBoletoFiltrado();

        if (idBoletoFiltrado != null) {

            cenariosAtivos =
                    filtrarPorIdBoleto(
                            cenariosAtivos,
                            idBoletoFiltrado
                    );
        }

        validarCenariosEncontrados(
                cenariosAtivos,
                idBoletoFiltrado
        );

        validarExecucaoEmMassa(
                cenariosAtivos,
                idBoletoFiltrado
        );

        return converterParaDataProvider(
                cenariosAtivos
        );
    }

    private static List<CenarioPagamentoBoleto>
    filtrarCenariosAtivos(
            List<CenarioPagamentoBoleto> cenarios
    ) {

        List<CenarioPagamentoBoleto> ativos =
                new ArrayList<>();

        for (
                CenarioPagamentoBoleto cenario
                : cenarios
        ) {

            if (cenario.isAtivo()) {
                ativos.add(
                        cenario
                );
            }
        }

        return ativos;
    }

    private static Long obterIdBoletoFiltrado() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_ID_BOLETO
                );

        if (valor == null || valor.isBlank()) {
            return null;
        }

        try {
            long idBoleto =
                    Long.parseLong(
                            valor.trim()
                    );

            if (idBoleto <= 0) {
                throw new IllegalArgumentException(
                        "A propriedade -DidBoleto "
                                + "deve ser maior que zero."
                );
            }

            return idBoleto;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "A propriedade -DidBoleto "
                            + "não contém um número válido: "
                            + valor,
                    exception
            );
        }
    }

    private static List<CenarioPagamentoBoleto>
    filtrarPorIdBoleto(
            List<CenarioPagamentoBoleto> cenarios,
            long idBoleto
    ) {

        List<CenarioPagamentoBoleto> filtrados =
                new ArrayList<>();

        for (
                CenarioPagamentoBoleto cenario
                : cenarios
        ) {

            if (cenario.getIdBoleto() == idBoleto) {
                filtrados.add(
                        cenario
                );
            }
        }

        return filtrados;
    }

    private static void validarCenariosEncontrados(
            List<CenarioPagamentoBoleto> cenarios,
            Long idBoletoFiltrado
    ) {

        if (!cenarios.isEmpty()) {
            return;
        }

        if (idBoletoFiltrado != null) {
            throw new IllegalStateException(
                    "Nenhum cenário ativo foi encontrado "
                            + "para o IdBoleto "
                            + idBoletoFiltrado
                            + "."
            );
        }

        throw new IllegalStateException(
                "Nenhum cenário ativo foi encontrado "
                        + "no arquivo de pagamentos."
        );
    }

    private static void validarExecucaoEmMassa(
            List<CenarioPagamentoBoleto> cenarios,
            Long idBoletoFiltrado
    ) {

        if (idBoletoFiltrado != null
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
                            + "Como os testes inserem pagamentos, "
                            + "a execução em massa precisa ser "
                            + "autorizada com "
                            + "-DpermitirExecucaoEmMassa=true. "
                            + "Para executar somente um boleto, "
                            + "utilize -DidBoleto=NUMERO."
            );
        }
    }

    private static boolean
    obterPermissaoDeExecucaoEmMassa() {

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

    private static Object[][]
    converterParaDataProvider(
            List<CenarioPagamentoBoleto> cenarios
    ) {

        Object[][] dados =
                new Object[cenarios.size()][1];

        for (
                int indice = 0;
                indice < cenarios.size();
                indice++
        ) {

            dados[indice][0] =
                    cenarios.get(
                            indice
                    );
        }

        return dados;
    }
}