package com.automation.features.quitacao.wait;

import com.automation.core.wait.PollingExecutor;
import com.automation.features.quitacao.model.BoletoEmitido;
import com.automation.features.quitacao.repository.BoletoEmitidoRepository;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.time.Duration;
import java.util.Optional;

public class BoletoEmitidoAwaiter {

    private static final Duration TIMEOUT_PADRAO =
            Duration.ofSeconds(15);

    private static final Duration INTERVALO_PADRAO =
            Duration.ofSeconds(1);

    private final BoletoEmitidoRepository repository;
    private final Duration timeout;
    private final Duration intervalo;

    public BoletoEmitidoAwaiter() {

        this(
                new BoletoEmitidoRepository(),
                TIMEOUT_PADRAO,
                INTERVALO_PADRAO
        );
    }

    public BoletoEmitidoAwaiter(
            BoletoEmitidoRepository repository
    ) {

        this(
                repository,
                TIMEOUT_PADRAO,
                INTERVALO_PADRAO
        );
    }

    public BoletoEmitidoAwaiter(
            BoletoEmitidoRepository repository,
            Duration timeout,
            Duration intervalo
    ) {

        validarDependencias(
                repository,
                timeout,
                intervalo
        );

        this.repository = repository;
        this.timeout = timeout;
        this.intervalo = intervalo;
    }

    @Step("Aguardar persistência do boleto na base de dados")
    public Optional<BoletoEmitido> aguardar(
            long idConta,
            long idBoleto
    ) throws Exception {

        PollingExecutor.Resultado<BoletoEmitido> resultado =
                PollingExecutor.aguardarAteEncontrar(
                        () -> repository
                                .buscarPorIdContaEIdBoleto(
                                        idConta,
                                        idBoleto
                                ),
                        timeout,
                        intervalo
                );

        anexarResultado(
                idConta,
                idBoleto,
                resultado
        );

        return resultado.getValor();
    }

    private void anexarResultado(
            long idConta,
            long idBoleto,
            PollingExecutor.Resultado<BoletoEmitido> resultado
    ) {

        String status =
                resultado.foiEncontrado()
                        ? "ENCONTRADO"
                        : "NÃO ENCONTRADO";

        String evidencia =
                "Status: "
                        + status
                        + System.lineSeparator()
                        + "IdConta: "
                        + idConta
                        + System.lineSeparator()
                        + "IdBoleto: "
                        + idBoleto
                        + System.lineSeparator()
                        + "Tentativas: "
                        + resultado.getTentativas()
                        + System.lineSeparator()
                        + "Tempo decorrido: "
                        + resultado
                                .getDuracao()
                                .toMillis()
                        + " ms"
                        + System.lineSeparator()
                        + "Timeout configurado: "
                        + timeout.toSeconds()
                        + " s"
                        + System.lineSeparator()
                        + "Intervalo configurado: "
                        + intervalo.toMillis()
                        + " ms";

        Allure.addAttachment(
                "Polling do boleto emitido",
                "text/plain",
                evidencia
        );
    }

    private void validarDependencias(
            BoletoEmitidoRepository repository,
            Duration timeout,
            Duration intervalo
    ) {

        if (repository == null) {
            throw new IllegalArgumentException(
                    "O BoletoEmitidoRepository não pode ser nulo."
            );
        }

        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {

            throw new IllegalArgumentException(
                    "O timeout deve ser maior que zero."
            );
        }

        if (intervalo == null
                || intervalo.isZero()
                || intervalo.isNegative()) {

            throw new IllegalArgumentException(
                    "O intervalo deve ser maior que zero."
            );
        }

        if (intervalo.compareTo(timeout) > 0) {
            throw new IllegalArgumentException(
                    "O intervalo não pode ser maior que o timeout."
            );
        }
    }
}
