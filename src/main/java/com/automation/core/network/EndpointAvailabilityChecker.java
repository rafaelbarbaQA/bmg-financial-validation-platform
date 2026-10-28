package com.automation.core.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

public class EndpointAvailabilityChecker {

    public EndpointAvailability check(
            String endpoint,
            int timeoutMillis
    ) {

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException(
                    "Endpoint nao pode ser vazio."
            );
        }

        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException(
                    "Timeout deve ser maior que zero."
            );
        }

        URI uri;

        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Endpoint invalido: " + endpoint,
                    exception
            );
        }

        String host = uri.getHost();

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                    "Nao foi possivel identificar o host: "
                            + endpoint
            );
        }

        int port = resolvePort(uri);

        long start = System.nanoTime();

        InetAddress address;

        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException exception) {

            return unavailable(
                    endpoint,
                    host,
                    port,
                    null,
                    "DNS_UNRESOLVED: "
                            + exception.getMessage(),
                    start
            );
        }

        try (Socket socket = new Socket()) {

            socket.connect(
                    new InetSocketAddress(
                            address,
                            port
                    ),
                    timeoutMillis
            );

            return new EndpointAvailability(
                    endpoint,
                    host,
                    port,
                    address.getHostAddress(),
                    true,
                    "TCP_CONNECTION_OK",
                    elapsedMillis(start)
            );

        } catch (IOException exception) {

            return unavailable(
                    endpoint,
                    host,
                    port,
                    address.getHostAddress(),
                    exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage(),
                    start
            );
        }
    }

    private int resolvePort(URI uri) {

        if (uri.getPort() > 0) {
            return uri.getPort();
        }

        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }

        if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }

        throw new IllegalArgumentException(
                "Nao foi possivel determinar a porta: "
                        + uri
        );
    }

    private EndpointAvailability unavailable(
            String endpoint,
            String host,
            int port,
            String address,
            String detail,
            long start
    ) {

        return new EndpointAvailability(
                endpoint,
                host,
                port,
                address,
                false,
                detail,
                elapsedMillis(start)
        );
    }

    private long elapsedMillis(long start) {

        return (
                System.nanoTime()
                        - start
        ) / 1_000_000;
    }
}