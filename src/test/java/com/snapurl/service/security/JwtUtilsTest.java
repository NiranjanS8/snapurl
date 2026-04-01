package com.snapurl.service.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {

    @Test
    void validateConfigurationRejectsBlankSecret() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "");

        IllegalStateException exception = assertThrows(IllegalStateException.class, jwtUtils::validateConfiguration);

        assertEquals("JWT secret is required. Set JWT_SECRET to a valid Base64-encoded key.", exception.getMessage());
    }

    @Test
    void validateConfigurationRejectsInvalidBase64Secret() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "not-base64");

        IllegalStateException exception = assertThrows(IllegalStateException.class, jwtUtils::validateConfiguration);

        assertEquals("JWT secret must be a valid Base64-encoded key.", exception.getMessage());
    }

    @Test
    void validateConfigurationRejectsShortDecodedSecret() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "c2hvcnQ=");

        IllegalStateException exception = assertThrows(IllegalStateException.class, jwtUtils::validateConfiguration);

        assertEquals("JWT secret must decode to at least 32 bytes.", exception.getMessage());
    }

    @Test
    void validateConfigurationAcceptsValidSecret() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        assertDoesNotThrow(jwtUtils::validateConfiguration);
    }
}
