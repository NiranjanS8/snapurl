package com.snapurl.service.security;

import com.snapurl.service.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class ShortenRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final long publicShortenPerMinute;
    private final long authShortenPerMinute;

    public ShortenRateLimitFilter(
            RateLimitService rateLimitService,
            @Value("${snapurl.rate-limit.public-shorten-per-minute:10}") long publicShortenPerMinute,
            @Value("${snapurl.rate-limit.auth-shorten-per-minute:30}") long authShortenPerMinute
    ) {
        this.rateLimitService = rateLimitService;
        this.publicShortenPerMinute = publicShortenPerMinute;
        this.authShortenPerMinute = authShortenPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();
        if ("/api/urls/public/shorten".equals(requestPath)) {
            String clientIp = extractClientIp(request);
            boolean allowed = rateLimitService.isAllowed(
                    "snapurl:rate-limit:public-shorten:" + clientIp,
                    publicShortenPerMinute,
                    Duration.ofMinutes(1)
            );
            if (!allowed) {
                writeRateLimitResponse(response, "Too many public shorten requests. Please try again in a minute.");
                return;
            }
        }

        if ("/api/urls/shorten".equals(requestPath)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String identity = authentication != null && authentication.isAuthenticated()
                    ? authentication.getName()
                    : extractClientIp(request);

            boolean allowed = rateLimitService.isAllowed(
                    "snapurl:rate-limit:auth-shorten:" + identity,
                    authShortenPerMinute,
                    Duration.ofMinutes(1)
            );
            if (!allowed) {
                writeRateLimitResponse(response, "Too many shorten requests. Please try again in a minute.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
