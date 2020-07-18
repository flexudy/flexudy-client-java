package com.flexudy.education.client.service.network;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HostResolverTest {

    @Test
    public void testResolveWithNullEnvironment() {
        assertThrows(NullPointerException.class, () -> HostResolver.resolve(null));
    }

    @Test
    public void testResolveForProductionEnvironment() {
        assertThat(HostResolver.resolve(Environment.PRODUCTION)).isEqualTo(HostResolver.PRODUCTION_URL);
    }

}
