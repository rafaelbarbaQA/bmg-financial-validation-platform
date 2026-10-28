package com.automation.core.soap;

import java.io.IOException;

public class SoapClientException extends IOException {

    public enum TipoFalha {
        DNS,
        TIMEOUT,
        CONEXAO_INDISPONIVEL,
        HTTP,
        SOAP_FAULT,
        COMUNICACAO
    }

    private final TipoFalha tipoFalha;
    private final String endpoint;
    private final Integer statusHttp;
    private final String responseBody;

    public SoapClientException(
            TipoFalha tipoFalha,
            String mensagem,
            String endpoint,
            Integer statusHttp,
            String responseBody,
            Throwable causa
    ) {
        super(mensagem, causa);

        this.tipoFalha = tipoFalha;
        this.endpoint = endpoint;
        this.statusHttp = statusHttp;
        this.responseBody = responseBody;
    }

    public TipoFalha getTipoFalha() {
        return tipoFalha;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
