package com.automation.core.soap;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SoapGatewayTest {

    @DataProvider(name = "cenariosEndpoint")
    public Object[][] cenariosEndpoint() {

        return new Object[][]{
                {
                        "http://localhost:30088",
                        "Conta.asmx",
                        "http://localhost:30088/Conta.asmx"
                },
                {
                        "http://localhost:30088/",
                        "Conta.asmx",
                        "http://localhost:30088/Conta.asmx"
                },
                {
                        "http://localhost:30088",
                        "/Conta.asmx",
                        "http://localhost:30088/Conta.asmx"
                },
                {
                        "http://localhost:30088/",
                        "/Conta.asmx",
                        "http://localhost:30088/Conta.asmx"
                }
        };
    }

    @Test(
            dataProvider = "cenariosEndpoint",
            groups = {"unit", "soap"}
    )
    public void deveMontarEndpointSemDuplicarBarras(
            String wsUrl,
            String endpointServico,
            String endpointEsperado
    ) {

        SoapGateway soapGateway =
                new SoapGateway(wsUrl);

        String endpointGerado =
                soapGateway.montarEndpoint(
                        endpointServico
                );

        Assert.assertEquals(
                endpointGerado,
                endpointEsperado,
                "O endpoint SOAP foi montado incorretamente."
        );
    }

    @Test(groups = {"unit", "soap"})
    public void deveRejeitarUrlBaseVazia() {

        IllegalStateException exception =
                Assert.expectThrows(
                        IllegalStateException.class,
                        () -> new SoapGateway(" ")
                );

        Assert.assertTrue(
                exception
                        .getMessage()
                        .contains("URL base"),
                "A mensagem da exceção está diferente do esperado."
        );
    }

    @Test(groups = {"unit", "soap"})
    public void deveRejeitarEndpointDoServicoVazio() {

        SoapGateway soapGateway =
                new SoapGateway(
                        "http://localhost:30088"
                );

        IllegalArgumentException exception =
                Assert.expectThrows(
                        IllegalArgumentException.class,
                        () -> soapGateway.montarEndpoint(" ")
                );

        Assert.assertTrue(
                exception
                        .getMessage()
                        .contains("endpoint do serviço"),
                "A mensagem da exceção está diferente do esperado."
        );
    }

    @Test(groups = {"unit", "soap"})
    public void deveRejeitarXmlRequestVazio() {

        SoapGateway soapGateway =
                new SoapGateway(
                        "http://localhost:30088"
                );

        IllegalArgumentException exception =
                Assert.expectThrows(
                        IllegalArgumentException.class,
                        () -> soapGateway.enviarRequisicaoSoap(
                                " ",
                                "http://localhost:30088/Conta.asmx",
                                null
                        )
                );

        Assert.assertTrue(
                exception
                        .getMessage()
                        .contains("XML da requisição"),
                "A mensagem da exceção está diferente do esperado."
        );
    }
}
