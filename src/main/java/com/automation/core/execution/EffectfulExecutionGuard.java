package com.automation.core.execution;

public final class EffectfulExecutionGuard {

    public static final String PROPRIEDADE_AUTORIZACAO =
            "permitirOperacoesComEfeito";

    private EffectfulExecutionGuard() {
        // Impede instanciacao.
    }

    public static void exigirAutorizacao() {

        String valor =
                System.getProperty(
                        PROPRIEDADE_AUTORIZACAO
                );

        if (valor == null
                || valor.trim().isEmpty()
                || "false".equalsIgnoreCase(valor)) {

            throw new IllegalStateException(
                    "A operacao foi bloqueada porque altera dados "
                            + "do ambiente. Autorize explicitamente "
                            + "com -D"
                            + PROPRIEDADE_AUTORIZACAO
                            + "=true."
            );
        }

        if (!"true".equalsIgnoreCase(valor)) {

            throw new IllegalArgumentException(
                    "A propriedade -D"
                            + PROPRIEDADE_AUTORIZACAO
                            + " deve conter true ou false. "
                            + "Valor recebido: "
                            + valor
            );
        }
    }
}