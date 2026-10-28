package com.automation.infrastructure.payment.execution;

import com.automation.application.payment.execution.PaymentExecutionResult;
import com.automation.application.payment.execution.PaymentExecutor;
import com.automation.domain.payment.instruction.PaymentInstruction;
import com.automation.features.pagamento.model.BoletoParaPagamento;
import com.automation.features.pagamento.model.PagamentoBoleto;
import com.automation.features.pagamento.repository.BoletoParaPagamentoRepository;
import com.automation.features.pagamento.service.PagamentoBoletoService;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public final class RealPaymentExecutor
        implements PaymentExecutor {

    private static final String PROPRIEDADE_MODO =
            "financial.mode";

    private static final String MODO_REAL =
            "REAL";

    private final long idBoleto;
    private final LocalDate dataPagamento;
    private final BoletoParaPagamentoRepository boletoRepository;
    private final PagamentoBoletoService pagamentoService;
    private final Clock clock;

    public RealPaymentExecutor(
            long idBoleto,
            LocalDate dataPagamento
    ) {

        this(
                idBoleto,
                dataPagamento,
                new BoletoParaPagamentoRepository(),
                new PagamentoBoletoService(),
                Clock.systemUTC()
        );
    }

    RealPaymentExecutor(
            long idBoleto,
            LocalDate dataPagamento,
            BoletoParaPagamentoRepository boletoRepository,
            PagamentoBoletoService pagamentoService,
            Clock clock
    ) {

        if (idBoleto <= 0) {
            throw new IllegalArgumentException(
                    "O IdBoleto deve ser maior que zero."
            );
        }

        this.idBoleto = idBoleto;

        this.dataPagamento = Objects.requireNonNull(
                dataPagamento,
                "A data do pagamento não pode ser nula."
        );

        this.boletoRepository = Objects.requireNonNull(
                boletoRepository,
                "BoletoParaPagamentoRepository não pode ser nulo."
        );

        this.pagamentoService = Objects.requireNonNull(
                pagamentoService,
                "PagamentoBoletoService não pode ser nulo."
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock não pode ser nulo."
        );
    }

    @Override
    @Step("Executar pagamento real da instrução financeira")
    public PaymentExecutionResult execute(
            PaymentInstruction instruction
    ) {

        Objects.requireNonNull(
                instruction,
                "Payment instruction cannot be null."
        );

        validarModoReal();

        try {

            BoletoParaPagamento boleto =
                    consultarBoleto();

            validarConta(
                    instruction,
                    boleto
            );

            PagamentoBoleto pagamento =
                    criarPagamento(
                            instruction,
                            boleto
                    );

            registrarParametros(
                    instruction,
                    boleto,
                    pagamento
            );

            long idEventoPagamento =
                    pagamentoService.inserir(
                            pagamento
                    );

            Instant executedAt =
                    clock.instant();

            Allure.parameter(
                    "idEventoPagamento",
                    idEventoPagamento
            );

            Allure.step(
                    "Pagamento real inserido com IdEventoPagamento: "
                            + idEventoPagamento
            );

            return PaymentExecutionResult.success(
                    instruction,
                    Long.toString(idEventoPagamento),
                    "Pagamento real inserido em EventosExternosPagamentos.",
                    executedAt
            );

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Falha de banco de dados durante "
                            + "a execução real do pagamento.",
                    exception
            );
        }
    }

    @Step("Validar modo REAL")
    private void validarModoReal() {

        String modo =
                System.getProperty(
                        PROPRIEDADE_MODO,
                        ""
                );

        if (!MODO_REAL.equalsIgnoreCase(
                modo.trim()
        )) {

            throw new IllegalStateException(
                    "A execução real foi bloqueada. "
                            + "Utilize -Dfinancial.mode=REAL."
            );
        }
    }

    @Step("Consultar boleto que receberá o pagamento")
    private BoletoParaPagamento consultarBoleto()
            throws SQLException {

        Optional<BoletoParaPagamento> resultado =
                boletoRepository.buscarPorIdBoleto(
                        idBoleto
                );

        if (resultado.isEmpty()) {

            throw new IllegalStateException(
                    "Nenhum boleto foi localizado para "
                            + "o IdBoleto "
                            + idBoleto
                            + "."
            );
        }

        return resultado.get();
    }

    @Step("Validar vínculo entre instrução financeira e boleto")
    private void validarConta(
            PaymentInstruction instruction,
            BoletoParaPagamento boleto
    ) {

        String contaInstrucao =
                instruction
                        .getAccountId()
                        .getValue();

        String contaBoleto =
                Long.toString(
                        boleto.getIdConta()
                );

        if (!contaInstrucao.equals(
                contaBoleto
        )) {

            throw new IllegalStateException(
                    "O boleto "
                            + boleto.getIdBoleto()
                            + " pertence à conta "
                            + boleto.getIdConta()
                            + ", mas a instrução pertence à conta "
                            + contaInstrucao
                            + "."
            );
        }
    }

    @Step("Converter instrução financeira em pagamento de boleto")
    private PagamentoBoleto criarPagamento(
            PaymentInstruction instruction,
            BoletoParaPagamento boleto
    ) {

        BigDecimal valorPagamento =
                instruction.getAmount();

        BigDecimal valorBoleto =
                boleto.getValorBoleto();

        int comparacao =
                valorPagamento.compareTo(
                        valorBoleto
                );

        if (comparacao > 0) {

            throw new IllegalStateException(
                    "Execução real acima do valor da fatura "
                            + "ainda não está habilitada. "
                            + "Valor solicitado: "
                            + valorPagamento
                            + ". Valor do boleto: "
                            + valorBoleto
                            + "."
            );
        }

        if (comparacao == 0) {

            return PagamentoBoleto.total(
                    boleto,
                    dataPagamento
            );
        }

        return PagamentoBoleto.parcial(
                boleto,
                dataPagamento,
                valorPagamento
        );
    }

    private void registrarParametros(
            PaymentInstruction instruction,
            BoletoParaPagamento boleto,
            PagamentoBoleto pagamento
    ) {

        Allure.parameter(
                "modoExecucao",
                "REAL"
        );

        Allure.parameter(
                "idConta",
                instruction
                        .getAccountId()
                        .getValue()
        );

        Allure.parameter(
                "idBoleto",
                boleto.getIdBoleto()
        );

        Allure.parameter(
                "nossoNumero",
                boleto.getNossoNumero()
        );

        Allure.parameter(
                "cenario",
                instruction.getScenario()
        );

        Allure.parameter(
                "modalidade",
                pagamento.getModalidade()
        );

        Allure.parameter(
                "valorPagamento",
                pagamento.getValorPagamento()
        );

        Allure.parameter(
                "dataPagamento",
                dataPagamento
        );
    }
}