package com.automation.core.soap;

import com.automation.core.config.ConfigReader;
import com.automation.core.report.AllureAttachment;

import org.apache.hc.client5.http.classic.methods.HttpPost;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io
        .PoolingHttpClientConnectionManagerBuilder;

import org.apache.hc.core5.http.HttpEntity;

import org.apache.hc.core5.http.io.entity.StringEntity;

import org.apache.hc.core5.util.Timeout;

import java.io.IOException;

import java.nio.charset.StandardCharsets;

public class SoapClient implements AutoCloseable {

    private static final Timeout CONNECT_TIMEOUT =
            carregarTimeout(
                    "SOAP_CONNECT_TIMEOUT_SECONDS"
            );

    private static final Timeout RESPONSE_TIMEOUT =
            carregarTimeout(
                    "SOAP_RESPONSE_TIMEOUT_SECONDS"
            );

    private static final Timeout CONNECTION_REQUEST_TIMEOUT =
            carregarTimeout(
                    "SOAP_CONNECTION_REQUEST_TIMEOUT_SECONDS"
            );

    private final String endpointUrl;

    private final CloseableHttpClient httpClient;

    public SoapClient(
            String endpointUrl
    ) {

        this.endpointUrl =
                endpointUrl;

        ConnectionConfig connectionConfig =
                ConnectionConfig.custom()
                        .setConnectTimeout(
                                CONNECT_TIMEOUT
                        )
                        .setSocketTimeout(
                                RESPONSE_TIMEOUT
                        )
                        .build();

        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder
                        .create()
                        .setDefaultConnectionConfig(
                                connectionConfig
                        )
                        .build();

        RequestConfig requestConfig =
                RequestConfig.custom()
                        .setConnectionRequestTimeout(
                                CONNECTION_REQUEST_TIMEOUT
                        )
                        .setResponseTimeout(
                                RESPONSE_TIMEOUT
                        )
                        .build();

        this.httpClient =
                HttpClients.custom()
                        .setConnectionManager(
                                connectionManager
                        )
                        .setDefaultRequestConfig(
                                requestConfig
                        )
                        .build();
    }

    /**
     * Envia uma requisição SOAP 1.1 sem SOAPAction.
     */
    public String sendRequest(
            String soapRequest
    ) throws IOException {

        return sendRequest(
                soapRequest,
                null
        );
    }

    /**
     * Envia uma requisição SOAP 1.1 com SOAPAction opcional.
     *
     * @param soapRequest XML completo da requisição
     * @param soapAction  ação SOAP; pode ser nula
     * @return XML recebido na resposta
     */
    public String sendRequest(
            String soapRequest,
            String soapAction
    ) throws IOException {

        HttpPost httpPost =
                new HttpPost(
                        endpointUrl
                );

        httpPost.setHeader(
                "Content-Type",
                "text/xml; charset=UTF-8"
        );

        if (soapAction != null
                && !soapAction.isBlank()) {

            httpPost.setHeader(
                    "SOAPAction",
                    "\""
                            + soapAction
                            + "\""
            );
        }

        httpPost.setEntity(
                new StringEntity(
                        soapRequest,
                        StandardCharsets.UTF_8
                )
        );

        anexarEvidenciasDaRequisicao(
                soapRequest,
                soapAction
        );

        try {
            return httpClient.execute(
                    httpPost,
                    response -> {

                        int statusCode =
                                response.getCode();

                        HttpEntity responseEntity =
                                response.getEntity();

                        String responseBody =
                                responseEntity == null
                                        ? ""
                                        : new String(
                                                responseEntity
                                                        .getContent()
                                                        .readAllBytes(),
                                                StandardCharsets.UTF_8
                                        );

                        AllureAttachment
                                .anexarStatusHttp(
                                        String.valueOf(
                                                statusCode
                                        )
                                );

                        AllureAttachment
                                .anexarSoapResponse(
                                        responseBody
                                );

                        if (statusCode != 200) {
                            throw new IOException(
                                    "Erro HTTP "
                                            + statusCode
                            );
                        }

                        return responseBody;
                    }
            );

        } catch (IOException exception) {

            AllureAttachment
                    .anexarErroSoap(
                            exception
                                    .getClass()
                                    .getSimpleName()
                                    + ": "
                                    + exception.getMessage()
                    );

            throw exception;
        }
    }

    private void anexarEvidenciasDaRequisicao(
            String soapRequest,
            String soapAction
    ) {

        AllureAttachment
                .anexarSoapRequest(
                        soapRequest
                );

        String actionExibida =
                soapAction == null
                        || soapAction.isBlank()
                        ? "Não informada"
                        : soapAction;

        String dados =
                "Endpoint: "
                        + endpointUrl
                        + System.lineSeparator()
                        + "SOAPAction: "
                        + actionExibida
                        + System.lineSeparator()
                        + "Connect timeout: "
                        + CONNECT_TIMEOUT
                        + System.lineSeparator()
                        + "Response timeout: "
                        + RESPONSE_TIMEOUT
                        + System.lineSeparator()
                        + "Connection request timeout: "
                        + CONNECTION_REQUEST_TIMEOUT;

        AllureAttachment
                .anexarDadosSoap(
                        dados
                );
    }

    private static Timeout carregarTimeout(
            String propertyName
    ) {

        String configuredValue =
                ConfigReader.getProperty(
                        propertyName
                );

        try {
            long seconds =
                    Long.parseLong(
                            configuredValue
                    );

            if (seconds <= 0) {
                throw new IllegalStateException(
                        "O timeout "
                                + propertyName
                                + " deve ser maior que zero."
                );
            }

            return Timeout.ofSeconds(
                    seconds
            );

        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "O timeout "
                            + propertyName
                            + " deve possuir um número inteiro válido: "
                            + configuredValue,
                    exception
            );
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}