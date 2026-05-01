package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ClickEventDTO;
import com.snapurl.service.dtos.ShortenUrlRequest;
import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.dtos.UrlMappingPageDTO;
import com.snapurl.service.models.Users;
import com.snapurl.service.service.RateLimitResult;
import com.snapurl.service.service.ClientIpResolver;
import com.snapurl.service.service.RateLimitGuard;
import com.snapurl.service.service.AppMetricsService;
import com.snapurl.service.service.UrlAnalyticsService;
import com.snapurl.service.service.UrlMappingService;
import com.snapurl.service.service.UserService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@Slf4j
public class UrlMappingController {

    private final UrlMappingService urlMappingService;
    private final UrlAnalyticsService urlAnalyticsService;
    private final UserService userService;
    private final RateLimitGuard rateLimitGuard;
    private final ClientIpResolver clientIpResolver;
    private final AppMetricsService appMetricsService;
    @Value("${snapurl.rate-limit.public-shorten-per-minute:10}")
    private long publicShortenPerMinute;
    @Value("${snapurl.rate-limit.auth-shorten-per-minute:30}")
    private long authShortenPerMinute;
    public UrlMappingController(
            UrlMappingService urlMappingService,
            UrlAnalyticsService urlAnalyticsService,
            UserService userService,
            RateLimitGuard rateLimitGuard,
            ClientIpResolver clientIpResolver,
            AppMetricsService appMetricsService
    ) {
        this.urlMappingService = urlMappingService;
        this.urlAnalyticsService = urlAnalyticsService;
        this.userService = userService;
        this.rateLimitGuard = rateLimitGuard;
        this.clientIpResolver = clientIpResolver;
        this.appMetricsService = appMetricsService;
    }

    // {"originalUrl": "https://www.example.com/some/long/url"}
    // https://snapurl.com/v9AuvEYE  -->  https://www.example.com/some/long/url

    @PostMapping("/public/shorten")
    public ResponseEntity<?> createPublicShortUrl(@Valid @RequestBody ShortenUrlRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = clientIpResolver.resolve(httpServletRequest);
        RateLimitResult rateLimitResult = rateLimitGuard.check(
                "snapurl:rate-limit:public-shorten:" + clientIp,
                publicShortenPerMinute,
                Duration.ofMinutes(1),
                "urls_public_shorten",
                "Too many public shorten requests. Please try again in a minute."
        );

        UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(request.getOriginalUrl(), request.getCustomAlias(), null);
        appMetricsService.recordLinkCreated("public");
        log.info("Public shorten created shortUrl={} ip={}", urlMappingDTO.getShortUrl(), clientIp);
        return rateLimitGuard.withHeaders(ResponseEntity.ok(), rateLimitResult).body(urlMappingDTO);
    }

    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody ShortenUrlRequest request,
                                                        Principal principal) {

        Users user = userService.findByEmail(principal.getName());

        RateLimitResult rateLimitResult = rateLimitGuard.check(
                "snapurl:rate-limit:auth-shorten:" + principal.getName(),
                authShortenPerMinute,
                Duration.ofMinutes(1),
                "urls_authenticated_shorten",
                "Too many shorten requests. Please try again in a minute."
        );

        UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(request.getOriginalUrl(), request.getCustomAlias(), user);
        appMetricsService.recordLinkCreated("authenticated");
        log.info("Authenticated shorten created shortUrl={} email={}", urlMappingDTO.getShortUrl(), principal.getName());
        return rateLimitGuard.withHeaders(ResponseEntity.ok(), rateLimitResult).body(urlMappingDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteShortUrl(@PathVariable Long id, Principal principal) {
        Users user = userService.findByEmail(principal.getName());
        urlMappingService.deleteUrl(id, user);
        log.info("Short URL deleted id={} email={}", id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingPageDTO> getUserUrls(
            Principal principal,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer minClicks,
            @RequestParam(required = false) Integer maxClicks,
            @RequestParam(required = false) String status
    ) {
        Users user = userService.findByEmail(principal.getName());
        LocalDateTime parsedStartDate = startDate != null && !startDate.isBlank()
                ? LocalDate.parse(startDate).atStartOfDay()
                : null;
        LocalDateTime parsedEndDate = endDate != null && !endDate.isBlank()
                ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : null;
        UrlMappingPageDTO userUrls = urlMappingService.searchUserUrls(
                user,
                query,
                sortBy,
                order,
                cursor,
                size,
                parsedStartDate,
                parsedEndDate,
                minClicks,
                maxClicks,
                status
        );
        return ResponseEntity.ok(userUrls);
    }

    @GetMapping("analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventDTO>> getUrlAnalytics(@PathVariable String shortUrl, @RequestParam("startDate") String startDate,
                                                                   @RequestParam("endDate") String endDate, Principal principal)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        Users user = userService.findByEmail(principal.getName());
        List<ClickEventDTO> analytics = urlAnalyticsService.getClickEventByDate(shortUrl, start, end, user);
        log.info("URL analytics requested shortUrl={} email={}", shortUrl, principal.getName());
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate, Long>> getTotalClicksByDate(Principal principal,
                                                                     @RequestParam("startDate") String startDate,
                                                                      @RequestParam("endDate") String endDate)
    {
        Users user = userService.findByEmail(principal.getName());
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        Map<LocalDate, Long> totalClicks = urlAnalyticsService.getTotalClicksByUserAndDate(user, start, end);
        log.info("Total clicks analytics requested email={} startDate={} endDate={}", principal.getName(), startDate, endDate);
        return ResponseEntity.ok(totalClicks);
    }

}
