package com.automation.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties PROPERTIES =
            new Properties();

    private static final String ENVIRONMENT =
            obterAmbienteSelecionado();

    private static final String CONFIG_FILE =
            "config-" + ENVIRONMENT + ".properties";

    static {
        carregarConfiguracoes();
    }

    private ConfigReader() {
        // Impede instanciação.
    }

    public static String getProperty(String key) {

        validarChave(key);

        String systemProperty =
                System.getProperty(key);

        if (possuiValor(systemProperty)) {
            return systemProperty.trim();
        }

        String environmentVariable =
                System.getenv(key);

        if (possuiValor(environmentVariable)) {
            return environmentVariable.trim();
        }

        String propertyValue =
                PROPERTIES.getProperty(key);

        if (!possuiValor(propertyValue)) {
            throw new IllegalStateException(
                    "Propriedade não encontrada ou vazia: "
                            + key
                            + ". Arquivo carregado: "
                            + CONFIG_FILE
            );
        }

        return propertyValue.trim();
    }

    public static String getEnvironment() {
        return ENVIRONMENT;
    }

    private static void carregarConfiguracoes() {

        try (
                InputStream input =
                        ConfigReader.class
                                .getClassLoader()
                                .getResourceAsStream(CONFIG_FILE)
        ) {

            if (input == null) {
                throw new IllegalStateException(
                        "Arquivo de configuração não encontrado: "
                                + CONFIG_FILE
                                + ". Execute informando um ambiente válido, "
                                + "por exemplo: -Denv=141"
                );
            }

            PROPERTIES.load(input);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Erro ao carregar o arquivo: "
                            + CONFIG_FILE,
                    exception
            );
        }
    }

    private static String obterAmbienteSelecionado() {

        String environment =
                System.getProperty("env");

        if (!possuiValor(environment)) {
            throw new IllegalStateException(
                    "O ambiente não foi informado. "
                            + "Execute o Maven utilizando -Denv=128 "
                            + "ou -Denv=141."
            );
        }

        environment = environment.trim();

        if (!environment.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Nome de ambiente inválido: "
                            + environment
            );
        }

        return environment;
    }

    private static void validarChave(String key) {

        if (!possuiValor(key)) {
            throw new IllegalArgumentException(
                    "A chave da configuração não pode ser nula ou vazia."
            );
        }
    }

    private static boolean possuiValor(String value) {
        return value != null && !value.isBlank();
    }
}