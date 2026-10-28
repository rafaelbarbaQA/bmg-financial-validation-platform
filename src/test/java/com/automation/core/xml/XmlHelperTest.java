package com.automation.core.xml;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

import org.testng.Assert;
import org.testng.annotations.Test;

@Epic("Framework de Automação")
@Feature("Leitura segura de XML")
public class XmlHelperTest {

    @Test(
        description = "Deve obter elemento de XML com namespace",
        groups = {"unitario", "xml"}
    )
    @Severity(SeverityLevel.CRITICAL)
    public void deveObterValorEmXmlComNamespace() {

        String xml =
                "<soapenv:Envelope " +
                "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:con=\"http://conductor.com.br/\">" +
                    "<soapenv:Body>" +
                        "<con:Response>" +
                            "<con:CodRetorno>0</con:CodRetorno>" +
                        "</con:Response>" +
                    "</soapenv:Body>" +
                "</soapenv:Envelope>";

        String resultado =
                XmlHelper.obterValorPorNomeLocal(
                        xml,
                        "CodRetorno"
                );

        Assert.assertEquals(
                resultado,
                "0",
                "O valor de CodRetorno não foi interpretado corretamente."
        );
    }

    @Test(
        description = "Deve informar quando o elemento não existe",
        groups = {"unitario", "xml"}
    )
    public void deveFalharQuandoElementoNaoExiste() {

        String xml =
                "<resposta>" +
                    "<CodRetorno>0</CodRetorno>" +
                "</resposta>";

        IllegalStateException exception =
                Assert.expectThrows(
                        IllegalStateException.class,
                        () -> XmlHelper.obterValorPorNomeLocal(
                                xml,
                                "DescricaoRetorno"
                        )
                );

        Assert.assertTrue(
                exception
                        .getMessage()
                        .contains("DescricaoRetorno"),
                "A mensagem deveria informar o elemento ausente."
        );
    }

    @Test(
        description = "Deve rejeitar XML vazio",
        groups = {"unitario", "xml"}
    )
    public void deveRejeitarXmlVazio() {

        Assert.expectThrows(
                IllegalArgumentException.class,
                () -> XmlHelper.parsear("")
        );
    }

    @Test(
        description = "Deve rejeitar XML malformado",
        groups = {"unitario", "xml"}
    )
    public void deveRejeitarXmlMalformado() {

        String xml =
                "<resposta>" +
                    "<CodRetorno>0" +
                "</resposta>";

        Assert.expectThrows(
                IllegalArgumentException.class,
                () -> XmlHelper.parsear(xml)
        );
    }

    @Test(
        description = "Deve bloquear declaração de entidade externa",
        groups = {"unitario", "xml", "seguranca"}
    )
    @Severity(SeverityLevel.BLOCKER)
    public void deveBloquearTentativaDeXxe() {

        String xml =
                "<?xml version=\"1.0\"?>" +
                "<!DOCTYPE resposta [" +
                    "<!ENTITY arquivo SYSTEM " +
                    "\"file:///arquivo-inexistente\">" +
                "]>" +
                "<resposta>&arquivo;</resposta>";

        Assert.expectThrows(
                IllegalArgumentException.class,
                () -> XmlHelper.parsear(xml)
        );
    }
}
