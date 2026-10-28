package com.automation.tools.masspayment;

import com.automation.core.database.DatabaseAccessor;
import com.automation.core.execution.EffectfulExecutionGuard;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class QuitacaoSecondRoundMassPaymentRunnerIT {

    private static final int TIPO_BOLETO_QUITACAO = 9;
    private static final int ID_AUTORIZACAO = 9999;
    private static final int ID_OPERACAO = 99;
    private static final int STATUS_PENDENTE = 0;

    private static final String DB_IP_ESPERADO =
            "10.213.192.102";

    private static final String DB_NAME_ESPERADO =
            "BMG";

    private static final LocalDateTime DATA_PAGAMENTO =
            LocalDateTime.of(
                    2026,
                    7,
                    23,
                    16,
                    0,
                    0
            );

    private static final String DEFAULT_FILE =
            "src/test/resources/massas/quitacao-200.csv";

    /*
     * Confirma que estamos realmente na base 102.
     */
    private static final String VALIDAR_BANCO =
            "SELECT "
                    + "CONVERT(varchar(50), "
                    + "CONNECTIONPROPERTY('local_net_address')) AS DB_IP, "
                    + "DB_NAME() AS DB_NAME";

    /*
     * Como as contas do CSV sao exatamente as da segunda rodada,
     * buscamos o ultimo boleto de QUITACAO gerado para cada conta.
     */
    private static final String CONSULTAR_BOLETO =
            "SELECT TOP (1) "
                    + "Id_Boleto AS ID_BOLETO, "
                    + "Id_TipoBoleto AS ID_TIPO_BOLETO, "
                    + "Id_Conta AS ID_CONTA, "
                    + "ValorBoleto AS VALOR_BOLETO, "
                    + "CONVERT(varchar(30), NossoNumero) AS NOSSO_NUMERO "
                    + "FROM BoletosEmitidos WITH (NOLOCK) "
                    + "WHERE Id_Conta = ? "
                    + "AND Id_TipoBoleto = ? "
                    + "ORDER BY Id_Boleto DESC";

    /*
     * Quitacao nao permite novo pagamento para
     * a mesma conta + NossoNumero.
     */
    private static final String CONSULTAR_DUPLICIDADE =
            "SELECT TOP (1) "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO, "
                    + "Status AS STATUS "
                    + "FROM EventosExternosPagamentos WITH (NOLOCK) "
                    + "WHERE Id_Conta = ? "
                    + "AND NossoNumero = ? "
                    + "AND Id_Autorizacao = ? "
                    + "AND Id_Operacao = ? "
                    + "ORDER BY Id_EventoPagamento DESC";

    /*
     * Baseado no SQL manual informado.
     *
     * IMPORTANTE:
     * NossoNumero e ValorPago sao dinamicos,
     * vindos do boleto tipo 9 localizado.
     */
    private static final String INSERIR_PAGAMENTO =
            "SET NOCOUNT ON; "

                    + "DECLARE @Inseridos TABLE ("
                    + "Id_EventoPagamento int NOT NULL"
                    + "); "

                    + "INSERT INTO EventosExternosPagamentos ("
                    + "Banco, "
                    + "DataMovimento, "
                    + "DataPagamento, "
                    + "NossoNumero, "
                    + "ValorPago, "
                    + "NumeroDocumento, "
                    + "Cartao, "
                    + "Id_MovimentoPagamentoPendente, "
                    + "DefasagemCobranca, "
                    + "Id_Autorizacao, "
                    + "Id_Conta, "
                    + "Id_Operacao, "
                    + "DataEntradaPagamento, "
                    + "DataVencimentoPadrao, "
                    + "DataVencimentoReal, "
                    + "RestituicaoSaque, "
                    + "Status"
                    + ") "

                    + "OUTPUT INSERTED.Id_EventoPagamento "
                    + "INTO @Inseridos(Id_EventoPagamento) "

                    + "SELECT "
                    + "PROD.Banco, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "NULL, "
                    + "C.Cartao, "
                    + "NULL, "
                    + "NULL, "
                    + "?, "
                    + "EC.Id_Conta, "
                    + "?, "
                    + "?, "
                    + "EC.ProximoVencimentoPadrao, "
                    + "EC.ProximoVencimentoReal, "
                    + "0.00, "
                    + "? "

                    + "FROM EstadosContas EC WITH (NOLOCK) "

                    + "INNER JOIN Produtos PROD WITH (NOLOCK) "
                    + "ON PROD.Id_Produto = EC.Id_Produto "

                    + "CROSS APPLY ("
                    + "SELECT TOP (1) Cartao "
                    + "FROM Cartoes C WITH (NOLOCK) "
                    + "WHERE C.Id_Conta = EC.Id_Conta "
                    + "AND C.Portador = 1 "
                    + "ORDER BY C.SequencialCartao DESC"
                    + ") C "

                    + "WHERE EC.Id_Conta = ?; "

                    + "SELECT "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO "
                    + "FROM @Inseridos;";

    /*
     * Consulta final do registro que acabou de ser inserido.
     */
    private static final String CONSULTAR_PAGAMENTO_INSERIDO =
            "SELECT "
                    + "Id_EventoPagamento AS ID_EVENTO_PAGAMENTO, "
                    + "Banco AS BANCO, "
                    + "DataMovimento AS DATA_MOVIMENTO, "
                    + "DataPagamento AS DATA_PAGAMENTO, "
                    + "CONVERT(varchar(30), NossoNumero) AS NOSSO_NUMERO, "
                    + "ValorPago AS VALOR_PAGO, "
                    + "NumeroDocumento AS NUMERO_DOCUMENTO, "
                    + "RTRIM(Cartao) AS CARTAO, "
                    + "Id_Autorizacao AS ID_AUTORIZACAO, "
                    + "Id_Conta AS ID_CONTA, "
                    + "Id_Operacao AS ID_OPERACAO, "
                    + "DataEntradaPagamento AS DATA_ENTRADA_PAGAMENTO, "
                    + "DataVencimentoPadrao AS DATA_VENCIMENTO_PADRAO, "
                    + "DataVencimentoReal AS DATA_VENCIMENTO_REAL, "
                    + "RestituicaoSaque AS RESTITUICAO_SAQUE, "
                    + "Status AS STATUS "
                    + "FROM EventosExternosPagamentos WITH (NOLOCK) "
                    + "WHERE Id_EventoPagamento = ?";

    @Test(groups = {"effectful"})
    public void executarPagamentosQuitacaoSegundaRodada()
            throws Exception {

        boolean executar =
                Boolean.parseBoolean(
                        System.getProperty(
                                "payment.execute",
                                "false"
                        )
                );

        int quantidadeEsperada =
                Integer.parseInt(
                        System.getProperty(
                                "payment.expected",
                                "200"
                        )
                );

        Path arquivo =
                Paths.get(
                        System.getProperty(
                                "payment.mass.file",
                                DEFAULT_FILE
                        )
                );

        DatabaseAccessor database =
                new DatabaseAccessor();

        validarBanco(
                database
        );

        List<Long> contas =
                carregarContas(
                        arquivo
                );

        if (contas.size() != quantidadeEsperada) {

            throw new IllegalStateException(
                    "Quantidade incorreta. Esperado="
                            + quantidadeEsperada
                            + ", encontrado="
                            + contas.size()
            );
        }

        List<PagamentoPreparado> preparados =
                new ArrayList<PagamentoPreparado>();

        int duplicados = 0;

        for (Long idConta : contas) {

            PagamentoPreparado pagamento =
                    prepararPagamento(
                            database,
                            idConta
                    );

            Optional<Map<String, Object>> duplicidade =
                    database.executeQuery(
                            CONSULTAR_DUPLICIDADE,
                            pagamento.idConta,
                            new BigDecimal(
                                    pagamento.nossoNumero
                            ),
                            ID_AUTORIZACAO,
                            ID_OPERACAO
                    );

            if (duplicidade.isPresent()) {

                duplicados++;

                Map<String, Object> registro =
                        duplicidade.get();

                System.out.printf(
                        "DUPLICADO | Conta=%d | Boleto=%d | NossoNumero=%s | Evento=%s%n",
                        pagamento.idConta,
                        pagamento.idBoleto,
                        pagamento.nossoNumero,
                        registro.get(
                                "ID_EVENTO_PAGAMENTO"
                        )
                );

                continue;
            }

            preparados.add(
                    pagamento
            );
        }

        Path preview =
                gerarPreview(
                        preparados
                );

        System.out.println();
        System.out.println(
                "============================================================"
        );

        System.out.println(
                " PAGAMENTO QUITACAO - PRE-FLIGHT"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Contas recebidas...........: "
                        + contas.size()
        );

        System.out.println(
                "Boletos tipo 9 prontos.....: "
                        + preparados.size()
        );

        System.out.println(
                "Pagamentos duplicados......: "
                        + duplicados
        );

        System.out.println(
                "DataPagamento..............: "
                        + DATA_PAGAMENTO
        );

        System.out.println(
                "ValorPagamento.............: ValorBoleto"
        );

        System.out.println(
                "NossoNumero................: NossoNumero do boleto tipo 9"
        );

        System.out.println(
                "Preview CSV................: "
                        + preview.toAbsolutePath()
        );

        System.out.println(
                "Modo........................: "
                        + (
                        executar
                                ? "REAL"
                                : "PREVIEW"
                        )
        );

        System.out.println(
                "============================================================"
        );

        /*
         * Antes de qualquer INSERT, TODAS precisam
         * estar prontas e sem duplicidade.
         */
        if (duplicados != 0) {

            throw new IllegalStateException(
                    "Existem pagamentos duplicados. "
                            + "Nenhum INSERT foi executado."
            );
        }

        if (preparados.size() != contas.size()) {

            throw new IllegalStateException(
                    "Nem todas as contas ficaram prontas. "
                            + "Nenhum INSERT foi executado."
            );
        }

        if (!executar) {

            System.out.println();
            System.out.println(
                    "PREVIEW FINALIZADO - NENHUM PAGAMENTO INSERIDO."
            );

            return;
        }

        /*
         * Somente daqui para baixo existe escrita.
         */
        EffectfulExecutionGuard
                .exigirAutorizacao();

        Path relatorio =
                criarArquivoRelatorio();

        int sucessos = 0;
        int falhas = 0;

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                relatorio,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    "idConta;"
                            + "idBoleto;"
                            + "tipoBoleto;"
                            + "valorBoleto;"
                            + "nossoNumeroBoleto;"
                            + "idEventoPagamento;"
                            + "banco;"
                            + "dataMovimento;"
                            + "dataPagamento;"
                            + "nossoNumeroPagamento;"
                            + "valorPago;"
                            + "numeroDocumento;"
                            + "cartao;"
                            + "idAutorizacao;"
                            + "idOperacao;"
                            + "dataEntradaPagamento;"
                            + "dataVencimentoPadrao;"
                            + "dataVencimentoReal;"
                            + "restituicaoSaque;"
                            + "status;"
                            + "resultado;"
                            + "mensagem"
            );

            writer.newLine();
            writer.flush();

            for (
                    PagamentoPreparado pagamento
                    : preparados
            ) {

                try {

                    /*
                     * Segunda protecao contra duplicidade:
                     * verifica novamente imediatamente antes do INSERT.
                     */
                    Optional<Map<String, Object>> duplicidade =
                            database.executeQuery(
                                    CONSULTAR_DUPLICIDADE,
                                    pagamento.idConta,
                                    new BigDecimal(
                                            pagamento.nossoNumero
                                    ),
                                    ID_AUTORIZACAO,
                                    ID_OPERACAO
                            );

                    if (duplicidade.isPresent()) {

                        throw new IllegalStateException(
                                "Pagamento apareceu como duplicado "
                                        + "antes do INSERT."
                        );
                    }

                    long idEvento =
                            inserirPagamento(
                                    database,
                                    pagamento
                            );

                    Map<String, Object> evento =
                            consultarEventoInserido(
                                    database,
                                    idEvento
                            );

                    validarEventoInserido(
                            pagamento,
                            evento,
                            idEvento
                    );

                    escreverResultado(
                            writer,
                            pagamento,
                            evento,
                            "SUCCESS",
                            ""
                    );

                    sucessos++;

                    System.out.printf(
                            "SUCESSO | Conta=%d | Boleto=%d | Valor=%s | NossoNumero=%s | Evento=%d%n",
                            pagamento.idConta,
                            pagamento.idBoleto,
                            pagamento.valorBoleto.toPlainString(),
                            pagamento.nossoNumero,
                            idEvento
                    );

                } catch (Exception exception) {

                    falhas++;

                    escreverResultado(
                            writer,
                            pagamento,
                            null,
                            "FAILED",
                            exception.getMessage()
                    );

                    System.err.printf(
                            "ERRO | Conta=%d | Boleto=%d | %s%n",
                            pagamento.idConta,
                            pagamento.idBoleto,
                            exception.getMessage()
                    );
                }

                /*
                 * Checkpoint por conta.
                 */
                writer.flush();
            }
        }

        System.out.println();
        System.out.println(
                "============================================================"
        );

        System.out.println(
                " PAGAMENTO QUITACAO - SUMMARY"
        );

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "Contas.....................: "
                        + preparados.size()
        );

        System.out.println(
                "Sucesso....................: "
                        + sucessos
        );

        System.out.println(
                "Falha......................: "
                        + falhas
        );

        System.out.println(
                "DataPagamento..............: "
                        + DATA_PAGAMENTO
        );

        System.out.println(
                "Relatorio final............: "
                        + relatorio.toAbsolutePath()
        );

        System.out.println(
                "============================================================"
        );

        Assert.assertEquals(
                falhas,
                0,
                "Existem pagamentos com falha. "
                        + "Nao repita os pagamentos que tiveram SUCCESS."
        );
    }

    private static PagamentoPreparado prepararPagamento(
            DatabaseAccessor database,
            long idConta
    ) throws Exception {

        Optional<Map<String, Object>> resultado =
                database.executeQuery(
                        CONSULTAR_BOLETO,
                        idConta,
                        TIPO_BOLETO_QUITACAO
                );

        if (resultado.isEmpty()) {

            throw new IllegalStateException(
                    "Nenhum boleto tipo 9 encontrado "
                            + "para a conta "
                            + idConta
            );
        }

        Map<String, Object> registro =
                resultado.get();

        long idBoleto =
                numero(
                        registro,
                        "ID_BOLETO"
                ).longValue();

        long contaBoleto =
                numero(
                        registro,
                        "ID_CONTA"
                ).longValue();

        int tipoBoleto =
                numero(
                        registro,
                        "ID_TIPO_BOLETO"
                ).intValue();

        BigDecimal valorBoleto =
                bigDecimal(
                        registro,
                        "VALOR_BOLETO"
                );

        String nossoNumero =
                texto(
                        registro,
                        "NOSSO_NUMERO"
                );

        if (contaBoleto != idConta) {

            throw new IllegalStateException(
                    "Conta do boleto diferente da massa. "
                            + "Esperado="
                            + idConta
                            + ", encontrado="
                            + contaBoleto
            );
        }

        if (tipoBoleto != TIPO_BOLETO_QUITACAO) {

            throw new IllegalStateException(
                    "Boleto nao e tipo 9. Conta="
                            + idConta
            );
        }

        if (valorBoleto.signum() <= 0) {

            throw new IllegalStateException(
                    "ValorBoleto invalido. Conta="
                            + idConta
            );
        }

        if (nossoNumero.isBlank()) {

            throw new IllegalStateException(
                    "NossoNumero vazio. Conta="
                            + idConta
            );
        }

        /*
         * Garante que o NossoNumero realmente e numerico,
         * como esperado pela coluna de pagamento.
         */
        new BigDecimal(
                nossoNumero
        );

        return new PagamentoPreparado(
                idConta,
                idBoleto,
                tipoBoleto,
                valorBoleto,
                nossoNumero
        );
    }

    private static long inserirPagamento(
            DatabaseAccessor database,
            PagamentoPreparado pagamento
    ) throws Exception {

        Timestamp data =
                Timestamp.valueOf(
                        DATA_PAGAMENTO
                );

        Optional<Map<String, Object>> resultado =
                database.executeCommandReturningSingleRow(
                        INSERIR_PAGAMENTO,
                        data,
                        data,
                        new BigDecimal(
                                pagamento.nossoNumero
                        ),
                        pagamento.valorBoleto,
                        ID_AUTORIZACAO,
                        ID_OPERACAO,
                        data,
                        STATUS_PENDENTE,
                        pagamento.idConta
                );

        if (resultado.isEmpty()) {

            throw new IllegalStateException(
                    "INSERT nao retornou IdEventoPagamento."
            );
        }

        return numero(
                resultado.get(),
                "ID_EVENTO_PAGAMENTO"
        ).longValue();
    }

    private static Map<String, Object> consultarEventoInserido(
            DatabaseAccessor database,
            long idEvento
    ) throws Exception {

        Optional<Map<String, Object>> resultado =
                database.executeQuery(
                        CONSULTAR_PAGAMENTO_INSERIDO,
                        idEvento
                );

        if (resultado.isEmpty()) {

            throw new IllegalStateException(
                    "Evento inserido nao foi localizado. Id="
                            + idEvento
            );
        }

        return resultado.get();
    }

    private static void validarEventoInserido(
            PagamentoPreparado pagamento,
            Map<String, Object> evento,
            long idEvento
    ) {

        long eventoEncontrado =
                numero(
                        evento,
                        "ID_EVENTO_PAGAMENTO"
                ).longValue();

        long conta =
                numero(
                        evento,
                        "ID_CONTA"
                ).longValue();

        int autorizacao =
                numero(
                        evento,
                        "ID_AUTORIZACAO"
                ).intValue();

        int operacao =
                numero(
                        evento,
                        "ID_OPERACAO"
                ).intValue();

        int status =
                numero(
                        evento,
                        "STATUS"
                ).intValue();

        BigDecimal valorPago =
                bigDecimal(
                        evento,
                        "VALOR_PAGO"
                );

        String nossoNumeroPagamento =
                texto(
                        evento,
                        "NOSSO_NUMERO"
                );

        if (eventoEncontrado != idEvento) {

            throw new IllegalStateException(
                    "IdEventoPagamento divergente."
            );
        }

        if (conta != pagamento.idConta) {

            throw new IllegalStateException(
                    "IdConta divergente no pagamento."
            );
        }

        if (valorPago.compareTo(
                pagamento.valorBoleto
        ) != 0) {

            throw new IllegalStateException(
                    "ValorPago diferente de ValorBoleto. "
                            + "ValorBoleto="
                            + pagamento.valorBoleto
                            + ", ValorPago="
                            + valorPago
            );
        }

        if (
                new BigDecimal(
                        nossoNumeroPagamento
                ).compareTo(
                        new BigDecimal(
                                pagamento.nossoNumero
                        )
                ) != 0
        ) {

            throw new IllegalStateException(
                    "NossoNumero do pagamento diferente "
                            + "do NossoNumero do boleto."
            );
        }

        if (autorizacao != ID_AUTORIZACAO) {

            throw new IllegalStateException(
                    "IdAutorizacao diferente de 9999."
            );
        }

        if (operacao != ID_OPERACAO) {

            throw new IllegalStateException(
                    "IdOperacao diferente de 99."
            );
        }

        if (status != STATUS_PENDENTE) {

            throw new IllegalStateException(
                    "Status inicial diferente de 0."
            );
        }
    }

    private static void validarBanco(
            DatabaseAccessor database
    ) throws Exception {

        Optional<Map<String, Object>> resultado =
                database.executeQuery(
                        VALIDAR_BANCO
                );

        if (resultado.isEmpty()) {

            throw new IllegalStateException(
                    "Nao foi possivel identificar o banco."
            );
        }

        String ip =
                texto(
                        resultado.get(),
                        "DB_IP"
                );

        String databaseName =
                texto(
                        resultado.get(),
                        "DB_NAME"
                );

        System.out.println();
        System.out.println(
                "===== BANCO ALVO ====="
        );

        System.out.println(
                "DB IP........: " + ip
        );

        System.out.println(
                "DB NAME......: " + databaseName
        );

        if (!DB_IP_ESPERADO.equals(
                ip
        )) {

            throw new IllegalStateException(
                    "BANCO BLOQUEADO. Esperado "
                            + DB_IP_ESPERADO
                            + ", encontrado "
                            + ip
            );
        }

        if (!DB_NAME_ESPERADO.equalsIgnoreCase(
                databaseName
        )) {

            throw new IllegalStateException(
                    "Database incorreto: "
                            + databaseName
            );
        }
    }

    private static List<Long> carregarContas(
            Path arquivo
    ) throws Exception {

        if (!Files.exists(
                arquivo
        )) {

            throw new IllegalArgumentException(
                    "Arquivo nao encontrado: "
                            + arquivo.toAbsolutePath()
            );
        }

        List<String> linhas =
                Files.readAllLines(
                        arquivo,
                        StandardCharsets.UTF_8
                );

        if (linhas.size() < 2) {

            throw new IllegalArgumentException(
                    "Arquivo sem contas."
            );
        }

        String cabecalho =
                linhas.get(0)
                        .replace(
                                "\uFEFF",
                                ""
                        )
                        .trim();

        String[] colunas =
                cabecalho.split(
                        ";",
                        -1
                );

        int colunaConta =
                localizarColuna(
                        colunas,
                        "idConta"
                );

        int colunaAtivo =
                localizarColuna(
                        colunas,
                        "ativo"
                );

        List<Long> contas =
                new ArrayList<Long>();

        Set<Long> unicas =
                new HashSet<Long>();

        for (
                int linha = 1;
                linha < linhas.size();
                linha++
        ) {

            String texto =
                    linhas.get(linha)
                            .trim();

            if (texto.isEmpty()) {
                continue;
            }

            String[] valores =
                    texto.split(
                            ";",
                            -1
                    );

            boolean ativo =
                    Boolean.parseBoolean(
                            valores[colunaAtivo]
                                    .trim()
                    );

            if (!ativo) {
                continue;
            }

            long idConta =
                    Long.parseLong(
                            valores[colunaConta]
                                    .trim()
                    );

            if (idConta <= 0) {

                throw new IllegalArgumentException(
                        "IdConta invalido na linha "
                                + (linha + 1)
                );
            }

            if (!unicas.add(
                    idConta
            )) {

                throw new IllegalArgumentException(
                        "Conta duplicada: "
                                + idConta
                );
            }

            contas.add(
                    idConta
            );
        }

        return contas;
    }

    private static int localizarColuna(
            String[] colunas,
            String nome
    ) {

        for (
                int i = 0;
                i < colunas.length;
                i++
        ) {

            if (
                    nome.equalsIgnoreCase(
                            colunas[i]
                                    .replace(
                                            "\"",
                                            ""
                                    )
                                    .trim()
                    )
            ) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "Coluna nao encontrada: "
                        + nome
        );
    }

    private static Path gerarPreview(
            List<PagamentoPreparado> pagamentos
    ) throws Exception {

        Path pasta =
                Paths.get(
                        "target",
                        "quitacao-payment-results"
                );

        Files.createDirectories(
                pasta
        );

        Path arquivo =
                pasta.resolve(
                        "preview-pagamento-quitacao-"
                                + timestamp()
                                + ".csv"
                );

        try (
                BufferedWriter writer =
                        Files.newBufferedWriter(
                                arquivo,
                                StandardCharsets.UTF_8
                        )
        ) {

            writer.write(
                    "idConta;"
                            + "idBoleto;"
                            + "tipoBoleto;"
                            + "valorBoleto;"
                            + "nossoNumero;"
                            + "dataPagamento;"
                            + "status"
            );

            writer.newLine();

            for (
                    PagamentoPreparado pagamento
                    : pagamentos
            ) {

                writer.write(
                        pagamento.idConta
                                + ";"
                                + pagamento.idBoleto
                                + ";"
                                + pagamento.tipoBoleto
                                + ";"
                                + pagamento.valorBoleto.toPlainString()
                                + ";"
                                + pagamento.nossoNumero
                                + ";"
                                + DATA_PAGAMENTO
                                + ";READY"
                );

                writer.newLine();
            }
        }

        return arquivo;
    }

    private static Path criarArquivoRelatorio()
            throws Exception {

        Path pasta =
                Paths.get(
                        "target",
                        "quitacao-payment-results"
                );

        Files.createDirectories(
                pasta
        );

        return pasta.resolve(
                "relatorio-final-pagamento-quitacao-"
                        + timestamp()
                        + ".csv"
        );
    }

    private static void escreverResultado(
            BufferedWriter writer,
            PagamentoPreparado pagamento,
            Map<String, Object> evento,
            String resultado,
            String mensagem
    ) throws Exception {

        writer.write(
                pagamento.idConta
                        + ";"
                        + pagamento.idBoleto
                        + ";"
                        + pagamento.tipoBoleto
                        + ";"
                        + pagamento.valorBoleto.toPlainString()
                        + ";"
                        + pagamento.nossoNumero
                        + ";"
                        + csv(valor(evento, "ID_EVENTO_PAGAMENTO"))
                        + ";"
                        + csv(valor(evento, "BANCO"))
                        + ";"
                        + csv(valor(evento, "DATA_MOVIMENTO"))
                        + ";"
                        + csv(valor(evento, "DATA_PAGAMENTO"))
                        + ";"
                        + csv(valor(evento, "NOSSO_NUMERO"))
                        + ";"
                        + csv(valor(evento, "VALOR_PAGO"))
                        + ";"
                        + csv(valor(evento, "NUMERO_DOCUMENTO"))
                        + ";"
                        + csv(valor(evento, "CARTAO"))
                        + ";"
                        + csv(valor(evento, "ID_AUTORIZACAO"))
                        + ";"
                        + csv(valor(evento, "ID_OPERACAO"))
                        + ";"
                        + csv(valor(evento, "DATA_ENTRADA_PAGAMENTO"))
                        + ";"
                        + csv(valor(evento, "DATA_VENCIMENTO_PADRAO"))
                        + ";"
                        + csv(valor(evento, "DATA_VENCIMENTO_REAL"))
                        + ";"
                        + csv(valor(evento, "RESTITUICAO_SAQUE"))
                        + ";"
                        + csv(valor(evento, "STATUS"))
                        + ";"
                        + csv(resultado)
                        + ";"
                        + csv(mensagem)
        );

        writer.newLine();
    }

    private static Object valor(
            Map<String, Object> mapa,
            String coluna
    ) {

        if (mapa == null) {
            return "";
        }

        Object valor =
                mapa.get(
                        coluna
                );

        return valor == null
                ? ""
                : valor;
    }

    private static Number numero(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                registro.get(
                        coluna
                );

        if (!(valor instanceof Number)) {

            throw new IllegalStateException(
                    "Coluna "
                            + coluna
                            + " nao retornou numero: "
                            + valor
            );
        }

        return (Number) valor;
    }

    private static BigDecimal bigDecimal(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                registro.get(
                        coluna
                );

        if (valor == null) {

            throw new IllegalStateException(
                    "Coluna nula: "
                            + coluna
            );
        }

        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }

        return new BigDecimal(
                valor.toString()
        );
    }

    private static String texto(
            Map<String, Object> registro,
            String coluna
    ) {

        Object valor =
                registro.get(
                        coluna
                );

        if (valor == null) {

            throw new IllegalStateException(
                    "Coluna nula: "
                            + coluna
            );
        }

        return valor.toString()
                .trim();
    }

    private static String csv(
            Object valor
    ) {

        if (valor == null) {
            return "";
        }

        String texto =
                valor.toString()
                        .replace(
                                "\"",
                                "\"\""
                        )
                        .replace(
                                "\r",
                                " "
                        )
                        .replace(
                                "\n",
                                " "
                        );

        if (
                texto.contains(";")
                        || texto.contains("\"")
        ) {

            return "\""
                    + texto
                    + "\"";
        }

        return texto;
    }

    private static String timestamp() {

        return LocalDateTime.now()
                .format(
                        DateTimeFormatter.ofPattern(
                                "yyyyMMdd-HHmmss"
                        )
                );
    }

    private static final class PagamentoPreparado {

        private final long idConta;
        private final long idBoleto;
        private final int tipoBoleto;
        private final BigDecimal valorBoleto;
        private final String nossoNumero;

        private PagamentoPreparado(
                long idConta,
                long idBoleto,
                int tipoBoleto,
                BigDecimal valorBoleto,
                String nossoNumero
        ) {

            this.idConta = idConta;
            this.idBoleto = idBoleto;
            this.tipoBoleto = tipoBoleto;
            this.valorBoleto = valorBoleto;
            this.nossoNumero = nossoNumero;
        }
    }
}