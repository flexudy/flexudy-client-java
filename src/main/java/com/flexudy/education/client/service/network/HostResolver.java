package com.flexudy.education.client.service.network;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HostResolver {

    @VisibleForTesting
    static final String PRODUCTION_URL = "https://gateway.flexudy.com";

    @VisibleForTesting
    static final String SANDBOX_URL = "https://gateway-sandbox.flexudy.com";

    public static String resolve(@NonNull Environment environment) {
        switch (environment) {
            case SANDBOX:
                return SANDBOX_URL;
            case PRODUCTION:
            default:
                return PRODUCTION_URL;
        }
    }
}
