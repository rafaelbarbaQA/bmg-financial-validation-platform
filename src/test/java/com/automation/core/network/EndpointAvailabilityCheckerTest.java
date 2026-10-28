package com.automation.core.network;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.InetAddress;
import java.net.ServerSocket;

public class EndpointAvailabilityCheckerTest {

    @Test
    public void deveIdentificarEndpointDisponivel() throws Exception {

        try (
                ServerSocket serverSocket =
                        new ServerSocket(
                                0,
                                1,
                                InetAddress.getLoopbackAddress()
                        )
        ) {

            int port =
                    serverSocket.getLocalPort();

            String endpoint =
                    "http://127.0.0.1:"
                            + port
                            + "/Conta.asmx";

            EndpointAvailability result =
                    new EndpointAvailabilityChecker()
                            .check(
                                    endpoint,
                                    1000
                            );

            Assert.assertTrue(
                    result.isAvailable(),
                    "Endpoint local deveria estar disponivel."
            );

            Assert.assertEquals(
                    result.getPort(),
                    port
            );

            Assert.assertEquals(
                    result.getDetail(),
                    "TCP_CONNECTION_OK"
            );
        }
    }

    @Test
    public void deveIdentificarEndpointIndisponivel() throws Exception {

        int port;

        try (
                ServerSocket serverSocket =
                        new ServerSocket(
                                0,
                                1,
                                InetAddress.getLoopbackAddress()
                        )
        ) {

            port =
                    serverSocket.getLocalPort();
        }

        String endpoint =
                "http://127.0.0.1:"
                        + port
                        + "/Conta.asmx";

        EndpointAvailability result =
                new EndpointAvailabilityChecker()
                        .check(
                                endpoint,
                                1000
                        );

        Assert.assertFalse(
                result.isAvailable(),
                "Endpoint local fechado deveria estar indisponivel."
        );

        Assert.assertEquals(
                result.getPort(),
                port
        );

        Assert.assertNotNull(
                result.getDetail()
        );

        Assert.assertFalse(
                result.getDetail().isBlank()
        );
    }
}