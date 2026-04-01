package com.snapurl.service.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthControllerTest {

    @Test
    void healthReturnsOkStatus() {
        HealthController controller = new HealthController();

        var response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ok", response.getBody().get("status"));
    }
}
