package com.automation.core.config;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ConfigReaderTest {

    private static final String PROFILE_MARKER =
            "CONFIG_PROFILE_MARKER";

    @Test
    public void deveCarregarArquivoDoAmbienteSelecionado() {

        Assert.assertEquals(
                ConfigReader.getEnvironment(),
                "test"
        );

        Assert.assertEquals(
                ConfigReader.getProperty(PROFILE_MARKER),
                "test-profile"
        );
    }

    @Test
    public void devePriorizarPropriedadeInformadaPelaJvm() {

        String originalValue =
                System.getProperty(PROFILE_MARKER);

        try {
            System.setProperty(
                    PROFILE_MARKER,
                    "jvm-override"
            );

            Assert.assertEquals(
                    ConfigReader.getProperty(PROFILE_MARKER),
                    "jvm-override"
            );

        } finally {
            if (originalValue == null) {
                System.clearProperty(PROFILE_MARKER);
            } else {
                System.setProperty(
                        PROFILE_MARKER,
                        originalValue
                );
            }
        }
    }

    @Test
    public void deveFalharQuandoPropriedadeNaoExiste() {

        IllegalStateException exception =
                Assert.expectThrows(
                        IllegalStateException.class,
                        () -> ConfigReader.getProperty(
                                "PROPRIEDADE_INEXISTENTE"
                        )
                );

        Assert.assertTrue(
                exception.getMessage()
                        .contains(
                                "Propriedade não encontrada ou vazia"
                        )
        );
    }
}