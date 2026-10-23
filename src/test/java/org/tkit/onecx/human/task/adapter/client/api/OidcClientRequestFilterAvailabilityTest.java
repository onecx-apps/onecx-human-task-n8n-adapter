package org.tkit.onecx.human.task.adapter.client.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class OidcClientRequestFilterAvailabilityTest {

    @Test
    void oidcClientRequestFilterProviderIsOnClasspath() {
        assertDoesNotThrow(() -> Class.forName(
                "io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter",
                false,
                Thread.currentThread().getContextClassLoader()));
    }
}
