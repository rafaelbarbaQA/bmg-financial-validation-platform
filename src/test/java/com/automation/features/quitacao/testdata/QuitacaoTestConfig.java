package com.automation.features.quitacao.testdata;

import com.automation.core.config.ConfigReader;

public final class QuitacaoTestConfig {

    private static final String ID_CONTA_SMOKE =
            "QUITACAO_ID_CONTA_SMOKE";

    private static final String TIPO_CORRESPONDENCIA =
            "QUITACAO_TIPO_CORRESPONDENCIA";

    private QuitacaoTestConfig() {
        // Impede instanciação.
    }

    public static long getIdContaSmoke() {

        String value =
                ConfigReader.getProperty(
                        ID_CONTA_SMOKE
                );

        try {
            long idConta =
                    Long.parseLong(value);

            if (idConta <= 0) {
                throw new IllegalStateException(
                        "QUITACAO_ID_CONTA_SMOKE deve ser maior que zero."
                );
            }

            return idConta;

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "QUITACAO_ID_CONTA_SMOKE deve possuir um número válido: "
                            + value,
                    exception
            );
        }
    }

    public static String getTipoCorrespondencia() {
        return ConfigReader.getProperty(
                TIPO_CORRESPONDENCIA
        );
    }
}