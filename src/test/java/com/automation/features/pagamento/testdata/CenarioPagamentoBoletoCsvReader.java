package com.automation.features.pagamento.testdata;

import com.automation.features.pagamento.model.ModalidadePagamento;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CenarioPagamentoBoletoCsvReader {

    private static final String DIRETORIO =
            "testdata/pagamento/";

    private static final String PREFIXO_ARQUIVO =
            "cenarios-pagamento-";

    private static final String EXTENSAO_ARQUIVO =
            ".csv";

    private static final String COLUNA_NOME =
            "nome";

    private static final String COLUNA_ID_BOLETO =
            "idBoleto";

    private static final String COLUNA_MODALIDADE =
            "modalidade";

    private static final String COLUNA_ATIVO =
            "ativo";

    private final String caminhoRecurso;

    public CenarioPagamentoBoletoCsvReader() {

        this(
                montarCaminhoPadrao()
        );
    }

    public CenarioPagamentoBoletoCsvReader(
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

    public List<CenarioPagamentoBoleto> carregar() {

        InputStream arquivo =
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
                InputStream inputStream = arquivo;

                Reader reader =
                        new InputStreamReader(
                                inputStream,
                                StandardCharsets.UTF_8
                        );

                CSVParser parser =
                        formato.parse(
                                reader
                        )
        ) {

            validarCabecalhos(
                    parser
            );

            List<CenarioPagamentoBoleto> cenarios =
                    new ArrayList<>();

            Set<String> chavesEncontradas =
                    new HashSet<>();

            for (CSVRecord registro : parser) {

                long numeroLinha =
                        registro.getRecordNumber() + 1;

                CenarioPagamentoBoleto cenario =
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
                        "O arquivo CSV está vazio: "
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

        Set<String> cabecalhosEsperados =
                Set.of(
                        COLUNA_NOME,
                        COLUNA_ID_BOLETO,
                        COLUNA_MODALIDADE,
                        COLUNA_ATIVO
                );

        Set<String> cabecalhosEncontrados =
                parser.getHeaderMap()
                        .keySet();

        if (!cabecalhosEncontrados.equals(
                cabecalhosEsperados
        )) {

            throw new IllegalStateException(
                    "Cabeçalho inválido no arquivo CSV. "
                            + "Esperado: "
                            + cabecalhosEsperados
                            + ". Encontrado: "
                            + cabecalhosEncontrados
            );
        }
    }

    private CenarioPagamentoBoleto converterRegistro(
            CSVRecord registro,
            long numeroLinha
    ) {

        String nome =
                obterValorObrigatorio(
                        registro,
                        COLUNA_NOME,
                        numeroLinha
                );

        long idBoleto =
                converterIdBoleto(
                        registro,
                        numeroLinha
                );

        ModalidadePagamento modalidade =
                converterModalidade(
                        registro,
                        numeroLinha
                );

        boolean ativo =
                converterAtivo(
                        registro,
                        numeroLinha
                );

        return new CenarioPagamentoBoleto(
                nome,
                idBoleto,
                modalidade,
                ativo
        );
    }

    private long converterIdBoleto(
            CSVRecord registro,
            long numeroLinha
    ) {

        String valor =
                obterValorObrigatorio(
                        registro,
                        COLUNA_ID_BOLETO,
                        numeroLinha
                );

        try {
            long idBoleto =
                    Long.parseLong(
                            valor
                    );

            if (idBoleto <= 0) {
                throw new IllegalStateException(
                        "O IdBoleto deve ser maior que zero "
                                + "na linha "
                                + numeroLinha
                                + "."
                );
            }

            return idBoleto;

        } catch (NumberFormatException exception) {

            throw new IllegalStateException(
                    "O IdBoleto não contém um número válido "
                            + "na linha "
                            + numeroLinha
                            + ": "
                            + valor,
                    exception
            );
        }
    }

    private ModalidadePagamento converterModalidade(
            CSVRecord registro,
            long numeroLinha
    ) {

        String valor =
                obterValorObrigatorio(
                        registro,
                        COLUNA_MODALIDADE,
                        numeroLinha
                );

        try {
            return ModalidadePagamento.valueOf(
                    valor.toUpperCase(
                            Locale.ROOT
                    )
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "Modalidade não suportada na linha "
                            + numeroLinha
                            + ": "
                            + valor,
                    exception
            );
        }
    }

    private boolean converterAtivo(
            CSVRecord registro,
            long numeroLinha
    ) {

        String valor =
                obterValorObrigatorio(
                        registro,
                        COLUNA_ATIVO,
                        numeroLinha
                );

        if ("true".equalsIgnoreCase(valor)) {
            return true;
        }

        if ("false".equalsIgnoreCase(valor)) {
            return false;
        }

        throw new IllegalStateException(
                "A coluna ativo deve conter true ou false "
                        + "na linha "
                        + numeroLinha
                        + ": "
                        + valor
        );
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

        if (valor == null || valor.isBlank()) {
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

    private void validarDuplicidade(
            CenarioPagamentoBoleto cenario,
            Set<String> chavesEncontradas,
            long numeroLinha
    ) {

        String chave =
                cenario.getIdBoleto()
                        + "|"
                        + cenario.getModalidade();

        if (!chavesEncontradas.add(
                chave
        )) {

            throw new IllegalStateException(
                    "Cenário duplicado na linha "
                            + numeroLinha
                            + ". IdBoleto: "
                            + cenario.getIdBoleto()
                            + ", modalidade: "
                            + cenario.getModalidade()
            );
        }
    }

    private static String montarCaminhoPadrao() {

        String ambiente =
                System.getProperty(
                        "env",
                        "test"
                );

        if (ambiente == null
                || ambiente.isBlank()) {

            ambiente =
                    "test";
        }

        ambiente =
                ambiente.trim();

        if (!ambiente.matches(
                "[a-zA-Z0-9_-]+"
        )) {

            throw new IllegalArgumentException(
                    "O ambiente contém caracteres inválidos: "
                            + ambiente
            );
        }

        return DIRETORIO
                + PREFIXO_ARQUIVO
                + ambiente
                + EXTENSAO_ARQUIVO;
    }
}