package com.automation.core.report;

import io.qameta.allure.Attachment;

public final class AllureAttachment {

    private AllureAttachment() {
        // Impede a criação de objetos desta classe utilitária.
    }

    @Attachment(
        value = "SOAP Request",
        type = "application/xml",
        fileExtension = ".xml"
    )
    public static String anexarSoapRequest(String request) {
        return request;
    }

    @Attachment(
        value = "SOAP Response",
        type = "application/xml",
        fileExtension = ".xml"
    )
    public static String anexarSoapResponse(String response) {
        return response;
    }

    @Attachment(
        value = "Dados da requisição SOAP",
        type = "text/plain",
        fileExtension = ".txt"
    )
    public static String anexarDadosSoap(String dados) {
        return dados;
    }

    @Attachment(
        value = "HTTP Status",
        type = "text/plain",
        fileExtension = ".txt"
    )
    public static String anexarStatusHttp(String status) {
        return status;
    }

    @Attachment(
        value = "Erro na comunicação SOAP",
        type = "text/plain",
        fileExtension = ".txt"
    )
    public static String anexarErroSoap(String erro) {
        return erro;
    }

    @Attachment(
        value = "Resumo executivo - Pagamento",
        type = "text/plain",
        fileExtension = ".txt"
    )
    public static String anexarResumoExecutivoPagamento(
            String resumo
    ) {
        return resumo;
    }
}
