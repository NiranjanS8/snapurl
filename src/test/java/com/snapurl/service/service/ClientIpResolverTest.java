package com.snapurl.service.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTest {

    @Test
    void resolveUsesRemoteAddressWhenForwardedHeadersAreNotTrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.5");

        ClientIpResolver resolver = new ClientIpResolver(false);

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void resolveUsesFirstForwardedAddressWhenTrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.5");

        ClientIpResolver resolver = new ClientIpResolver(true);

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void resolveFallsBackToRemoteAddressWhenTrustedHeaderIsBlank() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(" ");
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        ClientIpResolver resolver = new ClientIpResolver(true);

        assertEquals("10.0.0.5", resolver.resolve(request));
    }
}
