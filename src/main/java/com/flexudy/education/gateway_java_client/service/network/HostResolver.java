package com.flexudy.education.gateway_java_client.service.network;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HostResolver {

    private static final String SANDBOX_URL = "https://sandbox-gateway.flexudy.com";
    private static final String PRODUCTION_URL = "https://gateway.flexudy.com";

    public static String resolve(Environment environment) {
        switch (environment) {
            case SANDBOX:
                return SANDBOX_URL;
            case PRODUCTION:
                return PRODUCTION_URL;
            default:
                throw new IllegalArgumentException(String.format("The environment %s is not supported", environment));
        }
    }
}
