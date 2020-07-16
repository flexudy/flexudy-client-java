package com.flexudy.education.gateway_java_client.service;

import lombok.Builder;
import lombok.Data;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class HttpClientConfig {

    private static final long DEFAULT_CONNECT_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(1);
    private static final long DEFAULT_WRITE_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(3);
    private static final long DEFAULT_READ_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(3);

    private Long connectTimeoutSeconds;
    private Long writeTimeoutSeconds;
    private Long readTimeoutSeconds;

    public long getConnectTimeoutSeconds() {
        return Optional.ofNullable(connectTimeoutSeconds).orElse(DEFAULT_CONNECT_TIMEOUT_SECONDS);
    }

    public long getWriteTimeoutSeconds() {
        return Optional.ofNullable(writeTimeoutSeconds).orElse(DEFAULT_WRITE_TIMEOUT_SECONDS);
    }

    public long getReadTimeoutSeconds() {
        return Optional.ofNullable(readTimeoutSeconds).orElse(DEFAULT_READ_TIMEOUT_SECONDS);
    }
}
