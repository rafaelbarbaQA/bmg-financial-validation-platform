package com.automation.core.execution;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EffectfulExecutionGuardTest {

    private String valorOriginal;

    @BeforeMethod
    public void guardarValorOriginal() {

        valorOriginal =
                System.getProperty(
                        EffectfulExecutionGuard
                                .PROPRIEDADE_AUTORIZACAO
                );
    }

    @AfterMethod
    public void restaurarValorOriginal() {

        String propriedade =
                EffectfulExecutionGuard
                        .PROPRIEDADE_AUTORIZACAO;

        if (valorOriginal == null) {
            System.clearProperty(propriedade);
            return;
        }

        System.setProperty(
                propriedade,
                valorOriginal
        );
    }

    @Test
    public void devePermitirOperacaoQuandoAutorizada() {

        System.setProperty(
                EffectfulExecutionGuard
                        .PROPRIEDADE_AUTORIZACAO,
                "true"
        );

        EffectfulExecutionGuard
                .exigirAutorizacao();
    }

    @Test
    public void deveBloquearOperacaoSemAutorizacao() {

        System.clearProperty(
                EffectfulExecutionGuard
                        .PROPRIEDADE_AUTORIZACAO
        );

        IllegalStateException exception =
                Assert.expectThrows(
                        IllegalStateException.class,
                        EffectfulExecutionGuard
                                ::exigirAutorizacao
                );

        Assert.assertTrue(
                exception.getMessage()
                        .contains(
                                "permitirOperacoesComEfeito=true"
                        )
        );
    }

    @Test
    public void deveBloquearValorDeAutorizacaoInvalido() {

        System.setProperty(
                EffectfulExecutionGuard
                        .PROPRIEDADE_AUTORIZACAO,
                "sim"
        );

        Assert.expectThrows(
                IllegalArgumentException.class,
                EffectfulExecutionGuard
                        ::exigirAutorizacao
        );
    }
}