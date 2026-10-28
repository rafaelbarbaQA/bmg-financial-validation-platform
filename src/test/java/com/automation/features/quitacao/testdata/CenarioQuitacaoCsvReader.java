package com.automation.features.quitacao.testdata;

import com.automation.core.config.ConfigReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CenarioQuitacaoCsvReader {

    private static final String DIRETORIO_MASSAS =
            "testdata/quitacao/";

    private static final String PREFIXO_ARQUIVO =
            "cenarios-quitacao-";

    private static final String EXTENSAO_ARQUIVO =
            ".csv";

    private static final String COLUNA_NOME =
            "nome";

    private static final String COLUNA_ID_CONTA =
            "idConta";

    private static final String COLUNA_TIPO_CORRESPONDENCIA =
            "tipoCorrespondencia";

    private static final String COLUNA_ATIVO =
            "ativo";

    private final String caminhoRecurso;

    public CenarioQuitacaoCsvReader() {

        this(
                montarCaminhoPadrao()
        );
    }

    public CenarioQuitacaoCsvReader(
            String caminhoRecurso
    ) {

        if (caminhoRecurso == null
                || caminhoRecurso.isBlank()) {

            throw new IllegalArgumentException(
                    "O caminho do arquivo CSV "
                            + "não pode ser vazio."
            );
        }

        this.caminhoRecurso =
                caminhoRecurso.trim();
    }

    private static String montarCaminhoPadrao() {

        return DIRETORIO_MASSAS
                + PREFIXO_ARQUIVO
                + ConfigReader.getEnvironment()
                + EXTENSAO_ARQUIVO;
    }

    public List<CenarioQuitacao> carregar() {

        InputStream inputStream =
                obterArquivo();

        CSVFormat formato =
                CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(';')
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .setIgnoreEmptyLines(true)
                        .get();

        try (
                InputStream arquivo = inputStream;

                Reader reader =
                        new InputStreamReader(
                                arquivo,
                                StandardCharsets.UTF_8
                        );

                CSVParser parser =
                        formato.parse(reader)
        ) {

            validarCabecalhos(
                    parser
            );

            List<CenarioQuitacao> cenarios =
                    new ArrayList<>();

            Set<String> chavesEncontradas =
                    new HashSet<>();

            for (CSVRecord registro : parser) {

                long numeroLinha =
                        registro.getRecordNumber()
                                + 1;

                CenarioQuitacao cenario =
                        converterRegistro(
                                registro,
                                numeroLinha
                        );

                validarDuplicidade(
                        cenario,
                        chavesEncontradas,
                        numeroLinha
                );

                cenarios.add(
                        cenario
                );
            }

            if (cenarios.isEmpty()) {
                throw new IllegalStateException(
                        "O arquivo de cenários está vazio: "
                                + caminhoRecurso
                );
            }

            return Collections.unmodifiableList(
                    cenarios
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Erro ao ler o arquivo CSV: "
                            + caminhoRecurso,
                    exception
            );
        }
    }

    private InputStream obterArquivo() {

        InputStream arquivo =
                Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream(
                                caminhoRecurso
                        );

        if (arquivo == null) {
            throw new IllegalStateException(
                    "Arquivo CSV não encontrado no classpath: "
                            + caminhoRecurso
            );
        }

        return arquivo;
    }

    private void validarCabecalhos(
            CSVParser parser
    ) {

        Set<String> esperados =
                new HashSet<>(
                        Arrays.asList(
                                COLUNA_NOME,
                                COLUNA_ID_CONTA,
                                COLUNA_TIPO_CORRESPONDENCIA,
                                COLUNA_ATIVO
                        )
                );

        Set<String> encontrados =
                parser.getHeaderMap()
                        .keySet();

        if (!encontrados.equals(
                esperados
        )) {
            throw new IllegalStateException(
                    "Cabeçalho inválido no arquivo CSV. "
                            + "Esperado: "
                            + esperados
                            + ". Encontrado: "
                            + encontrados
            );
        }
    }

    private CenarioQuitacao converterRegistro(
            CSVRecord registro,
            long numeroLinha
    ) {

        String nome =
                obterValorObrigatorio(
                        registro,
                        COLUNA_NOME,
                        numeroLinha
                );

        long idConta =
                converterLong(
                        registro,
                        COLUNA_ID_CONTA,
                        numeroLinha
                );

        String tipoCorrespondencia =
                obterValorObrigatorio(
                        registro,
                        COLUNA_TIPO_CORRESPONDENCIA,
                        numeroLinha
                );

        boolean ativo =
                converterBoolean(
                        registro,
                        COLUNA_ATIVO,
                        numeroLinha
                );

        return CenarioQuitacao
                .builder()
                .comNome(nome)
                .comIdConta(idConta)
                .comTipoCorrespondencia(
                        tipoCorrespondencia
                )
                .ativo(ativo)
                .build();
    }

    private String obterValorObrigatorio(
            CSVRecord registro,
            String coluna,
            long numeroLinha
    ) {

        String valor =
                registro.get(
                        coluna
                );

        if (valor == null
                || valor.isBlank()) {

            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " está vazia na linha "
                            + numeroLinha
                            + "."
            );
        }

        return valor.trim();
    }

    private long converterLong(
            CSVRecord registro,
            String coluna,
            long numeroLinha
    ) {

        String valor =
                obterValorObrigatorio(
                        registro,
                        coluna,
                        numeroLinha
                );

        try {
            long numero =
                    Long.parseLong(
                            valor
                    );

            if (numero <= 0) {
                throw new IllegalStateException(
                        "A coluna "
                                + coluna
                                + " deve ser maior que zero "
                                + "na linha "
                                + numeroLinha
                                + "."
                );
            }

            return numero;

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "A coluna "
                            + coluna
                            + " não contém um número válido "
                            + "na linha "
                            + numeroLinha
                            + ": "
                            + valor,
                    exception
            );
        }
    }

    private boolean converterBoolean(
            CSVRecord registro,
            String coluna,
            long numeroLinha
    ) {

        String valor =
                obterValorObrigatorio(
                        registro,
                        coluna,
                        numeroLinha
                );

        if ("true".equalsIgnoreCase(
                valor
        )) {
            return true;
        }

        if ("false".equalsIgnoreCase(
                valor
        )) {
            return false;
        }

        throw new IllegalStateException(
                "A coluna "
                        + coluna
                        + " deve conter true ou false "
                        + "na linha "
                        + numeroLinha
                        + ": "
                        + valor
        );
    }

    private void validarDuplicidade(
            CenarioQuitacao cenario,
            Set<String> chavesEncontradas,
            long numeroLinha
    ) {

        String chave =
                cenario.getIdConta()
                        + "|"
                        + cenario
                                .getTipoCorrespondencia();

        if (!chavesEncontradas.add(
                chave
        )) {
            throw new IllegalStateException(
                    "Cenário duplicado na linha "
                            + numeroLinha
                            + ". IdConta: "
                            + cenario.getIdConta()
                            + ", TipoCorrespondencia: "
                            + cenario
                                    .getTipoCorrespondencia()
            );
        }
    }
}