package com.automation.core.wait;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class PollingExecutor {

    private PollingExecutor() {
        // Impede instanciação.
    }

    @FunctionalInterface
    public interface OperacaoConsultavel<T> {

        Optional<T> executar() throws Exception;
    }

    public static <T> Resultado<T> aguardarAteEncontrar(
            OperacaoConsultavel<T> operacao,
            Duration timeout,
            Duration intervalo
    ) throws Exception {

        validarParametros(
                operacao,
                timeout,
                intervalo
        );

        long inicio =
                System.nanoTime();

        long timeoutEmNanos =
                timeout.toNanos();

        int tentativas = 0;

        while (true) {

            tentativas++;

            Optional<T> resultado =
                    operacao.executar();

            if (resultado == null) {
                throw new IllegalStateException(
                        "A operação de polling retornou null. "
                                + "Ela deve retornar Optional.empty() "
                                + "quando não encontrar o resultado."
                );
            }

            if (resultado.isPresent()) {

                return new Resultado<>(
                        resultado,
                        tentativas,
                        calcularDuracao(inicio)
                );
            }

            long tempoDecorrido =
                    System.nanoTime() - inicio;

            if (tempoDecorrido >= timeoutEmNanos) {

                return new Resultado<>(
                        Optional.empty(),
                        tentativas,
                        calcularDuracao(inicio)
                );
            }

            long tempoRestante =
                    timeoutEmNanos - tempoDecorrido;

            long tempoDeEspera =
                    Math.min(
                            intervalo.toNanos(),
                            tempoRestante
                    );

            aguardar(tempoDeEspera);
        }
    }

    private static void validarParametros(
            OperacaoConsultavel<?> operacao,
            Duration timeout,
            Duration intervalo
    ) {

        if (operacao == null) {
            throw new IllegalArgumentException(
                    "A operação de polling não pode ser nula."
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
    }

    private static void aguardar(
            long tempoEmNanos
    ) throws InterruptedException {

        try {
            TimeUnit.NANOSECONDS.sleep(
                    tempoEmNanos
            );

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();

            throw exception;
        }
    }

    private static Duration calcularDuracao(
            long inicio
    ) {

        long tempoDecorrido =
                System.nanoTime() - inicio;

        return Duration.ofNanos(
                Math.max(
                        tempoDecorrido,
                        0
                )
        );
    }

    public static final class Resultado<T> {

        private final Optional<T> valor;
        private final int tentativas;
        private final Duration duracao;

        private Resultado(
                Optional<T> valor,
                int tentativas,
                Duration duracao
        ) {

            this.valor = valor;
            this.tentativas = tentativas;
            this.duracao = duracao;
        }

        public Optional<T> getValor() {
            return valor;
        }

        public boolean foiEncontrado() {
            return valor.isPresent();
        }

        public int getTentativas() {
            return tentativas;
        }

        public Duration getDuracao() {
            return duracao;
        }
    }
}
