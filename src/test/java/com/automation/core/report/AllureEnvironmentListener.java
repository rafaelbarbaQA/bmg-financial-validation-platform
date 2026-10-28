package com.automation.core.report;

import org.testng.IExecutionListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Properties;

public final class AllureEnvironmentListener
        implements IExecutionListener {

    private static final String DIRETORIO_PADRAO =
            "target/allure-results";

    @Override
    public void onExecutionStart() {

        gerarArquivoDeAmbiente();
    }

    private void gerarArquivoDeAmbiente() {

        String diretorioConfigurado =
                System.getProperty(
                        "allure.results.directory",
                        DIRETORIO_PADRAO
                );

        Path diretorioResultados =
                Paths.get(diretorioConfigurado);

        Path arquivoAmbiente =
                diretorioResultados.resolve(
                        "environment.properties"
                );

        Properties propriedades =
                criarPropriedades();

        try {
            Files.createDirectories(
                    diretorioResultados
            );

            try (OutputStream outputStream =
                         Files.newOutputStream(
                                 arquivoAmbiente
                         )) {

                propriedades.store(
                        outputStream,
                        "Informacoes da execucao automatizada"
                );
            }

        } catch (IOException exception) {

            System.err.println(
                    "Não foi possível gerar o arquivo "
                            + "environment.properties: "
                            + exception.getMessage()
            );
        }
    }

    private Properties criarPropriedades() {

        Properties propriedades =
                new Properties();

        propriedades.setProperty(
                "Ambiente",
                System.getProperty(
                        "env",
                        "não informado"
                )
        );

        propriedades.setProperty(
                "Java",
                System.getProperty(
                        "java.version",
                        "não informado"
                )
        );

        propriedades.setProperty(
                "Sistema operacional",
                System.getProperty(
                        "os.name",
                        "não informado"
                )
        );

        propriedades.setProperty(
                "Versão do sistema operacional",
                System.getProperty(
                        "os.version",
                        "não informada"
                )
        );

        propriedades.setProperty(
                "Arquitetura",
                System.getProperty(
                        "os.arch",
                        "não informada"
                )
        );

        propriedades.setProperty(
                "Encoding",
                System.getProperty(
                        "file.encoding",
                        "não informado"
                )
        );

        propriedades.setProperty(
                "Timezone",
                ZoneId.systemDefault().getId()
        );

        propriedades.setProperty(
                "Tipo de execução",
                System.getenv("CI") == null
                        ? "Local"
                        : "CI/CD"
        );

        propriedades.setProperty(
                "Framework de testes",
                "TestNG"
        );

        propriedades.setProperty(
                "Relatório",
                "Allure Report"
        );

        return propriedades;
    }
}
