package com.automation.core.network;

public class EndpointAvailability {

    private final String endpoint;
    private final String host;
    private final int port;
    private final String resolvedAddress;
    private final boolean available;
    private final String detail;
    private final long elapsedMillis;

    public EndpointAvailability(
            String endpoint,
            String host,
            int port,
            String resolvedAddress,
            boolean available,
            String detail,
            long elapsedMillis
    ) {
        this.endpoint = endpoint;
        this.host = host;
        this.port = port;
        this.resolvedAddress = resolvedAddress;
        this.available = available;
        this.detail = detail;
        this.elapsedMillis = elapsedMillis;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getResolvedAddress() {
        return resolvedAddress;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getDetail() {
        return detail;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }
}