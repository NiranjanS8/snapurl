package com.snapurl.service.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final boolean trustForwardedHeader;

    public ClientIpResolver(@Value("${snapurl.rate-limit.trust-forwarded-header:false}") boolean trustForwardedHeader) {
        this.trustForwardedHeader = trustForwardedHeader;
    }

    public String resolve(HttpServletRequest request) {
        if (!trustForwardedHeader) {
            return request.getRemoteAddr();
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
