package com.automation.features.pagamento.repository;

import com.automation.core.database.DatabaseAccessor;
import com.automation.features.pagamento.model.PagamentoExistente;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

public class PagamentoProcessamentoRepository {

    private static final String CONSULTAR_POR_ID_EVENTO =
            "SELECT "
                    + "Id_EventoPagamento "
                    + "AS ID_EVENTO_PAGAMENTO, "
                    + "Status AS STATUS "
                    + "FROM EventosExternosPagamentos "
                    + "WHERE Id_EventoPagamento = ?";

    private final DatabaseAccessor databaseAccessor;

    public PagamentoProcessamentoRepository() {

        this(
                new DatabaseAccessor()
        );
    }

    public PagamentoProcessamentoRepository(
            DatabaseAccessor databaseAccessor
    ) {

        if (databaseAccessor == null) {
            throw new IllegalArgumentException(
                    "O DatabaseAccessor não pode ser nulo."
            );
        }

        this.databaseAccessor =
                databaseAccessor;
    }

    @Step("Consultar processamento do pagamento pelo IdEventoPagamento")
    public Optional<PagamentoExistente>
    buscarPorIdEventoPagamento(
            long idEventoPagamento
    ) throws SQLException {

        validarIdEventoPagamento(
                idEventoPagamento
        );

        Allure.parameter(
                "idEventoPagamento",
                idEventoPagamento
        );

        Optional<Map<String, Object>> resultado =
                databaseAccessor.executeQuery(
                        CONSULTAR_POR_ID_EVENTO,
                        idEventoPagamento
                );

        if (resultado.isEmpty()) {

            anexarResultadoNaoEncontrado(
                    idEventoPagamento
            );

            return Optional.empty();
        }

        PagamentoExistente pagamento =
                mapearPagamento(
                        resultado.get()
                );

        anexarPagamentoEncontrado(
                pagamento
        );

        return Optional.of(
                pagamento
        );
    }

    private PagamentoExistente mapearPagamento(
            Map<String, Object> registro
    ) {

        long idEventoPagamento =
                obterLong(
                        registro,
                        "ID_EVENTO_PAGAMENTO"
                );

        int status =
                obterInt(
                        registro,
                        "STATUS"
                );

        return new PagamentoExistente(
                idEventoPagamento,
                status
        );
    }

    private long obterLong(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        registro,
                        coluna
                );

        if (!(valor instanceof Number)) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não contém um valor numérico."
            );
        }

        return ((Number) valor)
                .longValue();
    }

    private int obterInt(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                obterValorObrigatorio(
                        registro,
                        coluna
                );

        if (!(valor instanceof Number)) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não contém um valor numérico."
            );
        }

        return ((Number) valor)
                .intValue();
    }

    private Object obterValorObrigatorio(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                registro.get(
                        coluna
                );

        if (valor == null) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não foi retornada "
                            + "ou possui valor nulo."
            );
        }

        return valor;
    }

    private void validarIdEventoPagamento(
            long idEventoPagamento
    ) {

        if (idEventoPagamento <= 0) {
            throw new IllegalArgumentException(
                    "O IdEventoPagamento deve ser "
                            + "maior que zero."
            );
        }
    }

    private void anexarResultadoNaoEncontrado(
            long idEventoPagamento
    ) {

        String evidencia =
                "IdEventoPagamento: "
                        + idEventoPagamento
                        + System.lineSeparator()
                        + "Resultado: NÃO ENCONTRADO";

        Allure.addAttachment(
                "Consulta do processamento do pagamento",
                "text/plain",
                evidencia
        );
    }

    private void anexarPagamentoEncontrado(
            PagamentoExistente pagamento
    ) {

        String evidencia =
                "IdEventoPagamento: "
                        + pagamento
                                .getIdEventoPagamento()
                        + System.lineSeparator()
                        + "Status: "
                        + pagamento.getStatus()
                        + System.lineSeparator()
                        + "Descrição: "
                        + pagamento
                                .getDescricaoStatus()
                        + System.lineSeparator()
                        + "Resultado: ENCONTRADO";

        Allure.addAttachment(
                "Consulta do processamento do pagamento",
                "text/plain",
                evidencia
        );
    }
}