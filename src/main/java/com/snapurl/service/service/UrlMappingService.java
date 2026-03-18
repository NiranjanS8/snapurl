package com.snapurl.service.service;

import com.google.common.net.InternetDomainName;
import com.snapurl.service.dtos.ClickEventDTO;
import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.dtos.UrlMappingPageDTO;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {

    public static final String INVALID_URL_MESSAGE = "We'll need a valid URL, like \"super-long-link.com/shorten-it\"";
    public static final String INVALID_ALIAS_MESSAGE = "Custom alias can only use letters, numbers, hyphens, or underscores and must be 3 to 32 characters long.";
    public static final String RESERVED_ALIAS_MESSAGE = "That alias is reserved. Please choose another one.";
    public static final String DUPLICATE_ALIAS_MESSAGE = "That short link alias is already in use. Try another one.";
    private static final String SHORT_URL_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int GENERATED_SHORT_URL_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    private static final String HOSTNAME_PATTERN = "^(?=.{4,253}$)(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,24}$";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "admin", "login", "register", "signup", "auth", "public", "dashboard", "error", "s"
    );

    private UrlMappingRepo urlMappingRepo;
    private ClickEventRepo clickEventRepo;
    private ClickAnalyticsDispatcher clickAnalyticsDispatcher;
    private ShortUrlCacheService shortUrlCacheService;
    private AnalyticsCacheService analyticsCacheService;

    public UrlMappingDTO createShortUrl(String originalUrl, String customAlias, Users user) {
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException(INVALID_URL_MESSAGE);
        }

        String normalizedAlias = normalizeAlias(customAlias);
        if (normalizedAlias != null) {
            validateCustomAlias(normalizedAlias);
            UrlMapping savedMapping = saveUrlMapping(originalUrl, normalizedAlias, user, true);
            shortUrlCacheService.put(savedMapping);
            return convertToDTO(savedMapping);
        }

        UrlMapping savedMapping = createWithGeneratedShortUrl(originalUrl, user);
        shortUrlCacheService.put(savedMapping);
        return convertToDTO(savedMapping);
    }

    private UrlMappingDTO convertToDTO(UrlMapping urlMapping) {
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setId(urlMapping.getId());
        dto.setOriginalUrl(urlMapping.getOriginalUrl());
        dto.setShortUrl(urlMapping.getShortUrl());
        dto.setClickCount(urlMapping.getClickCount());
        dto.setCreatedAt(urlMapping.getCreatedAt());
        dto.setLastAccessed(urlMapping.getLastAccessed());
        dto.setExpiresAt(urlMapping.getExpiresAt());
        dto.setStatus(isExpired(urlMapping) ? "expired" : "active");
        dto.setUsername(urlMapping.getUser() != null ? urlMapping.getUser().getUsername() : null);
        return dto;
    }
    private UrlMapping createWithGeneratedShortUrl(String originalUrl, Users user) {
        Set<String> attemptedCodes = new HashSet<>();

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String generatedShortUrl = generateShortUrl();
            if (!attemptedCodes.add(generatedShortUrl) || urlMappingRepo.existsByShortUrl(generatedShortUrl)) {
                continue;
            }

            try {
                return saveUrlMapping(originalUrl, generatedShortUrl, user, false);
            } catch (IllegalStateException ex) {
                if (!DUPLICATE_ALIAS_MESSAGE.equals(ex.getMessage())) {
                    throw ex;
                }
            }
        }

        throw new IllegalStateException("We couldn't create a unique short link right now. Please try again.");
    }

    private UrlMapping saveUrlMapping(String originalUrl, String shortUrl, Users user, boolean customAliasRequested) {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl.trim());
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedAt(LocalDateTime.now());
        urlMapping.setExpiresAt(LocalDateTime.now().plusYears(1));

        try {
            return urlMappingRepo.save(urlMapping);
        } catch (DataIntegrityViolationException ex) {
            if (customAliasRequested || urlMappingRepo.existsByShortUrl(shortUrl)) {
                throw new IllegalStateException(DUPLICATE_ALIAS_MESSAGE);
            }
            throw ex;
        }
    }

    private String generateShortUrl() {
        StringBuilder shortUrl = new StringBuilder(GENERATED_SHORT_URL_LENGTH);
        for (int i = 0; i < GENERATED_SHORT_URL_LENGTH; i++) {
            shortUrl.append(SHORT_URL_CHARACTERS.charAt(RANDOM.nextInt(SHORT_URL_CHARACTERS.length())));
        }
        return shortUrl.toString();
    }

    private String normalizeAlias(String customAlias) {
        if (customAlias == null) {
            return null;
        }

        String normalizedAlias = customAlias.trim();
        return normalizedAlias.isEmpty() ? null : normalizedAlias;
    }

    private void validateCustomAlias(String customAlias) {
        if (!customAlias.matches("^[A-Za-z0-9_-]{3,32}$")) {
            throw new IllegalArgumentException(INVALID_ALIAS_MESSAGE);
        }

        if (RESERVED_ALIASES.contains(customAlias.toLowerCase())) {
            throw new IllegalArgumentException(RESERVED_ALIAS_MESSAGE);
        }

        if (urlMappingRepo.existsByShortUrl(customAlias)) {
            throw new IllegalStateException(DUPLICATE_ALIAS_MESSAGE);
        }
    }

    private boolean isValidUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            return false;
        }

        String normalizedUrl = originalUrl.trim();
        String candidate = normalizedUrl.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")
                ? normalizedUrl
                : "https://" + normalizedUrl;

        try {
            URI uri = URI.create(candidate);
            String host = uri.getHost();
            return host != null
                    && !host.isBlank()
                    && host.matches(HOSTNAME_PATTERN)
                    && InternetDomainName.from(host).hasPublicSuffix();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public List<UrlMappingDTO> getUrlsByUser(Users user) {
        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        return urlMappings.stream().map(this::convertToDTO).toList();
    }

    public UrlMappingPageDTO searchUserUrls(
            Users user,
            String query,
            String sortBy,
            String order,
            String cursor,
            int size,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer minClicks,
            Integer maxClicks,
            String status
    ) {
        int safeSize = Math.min(Math.max(size, 1), 25);
        return urlMappingRepo.searchUserUrls(user, query, sortBy, order, cursor, safeSize, startDate, endDate, minClicks, maxClicks, status);
    }

    public List<ClickEventDTO> getClickEventByDate(String shortUrl, LocalDateTime start, LocalDateTime end) {
        List<ClickEventDTO> cachedAnalytics = analyticsCacheService.getUrlAnalytics(shortUrl, start, end);
        if (cachedAnalytics != null) {
            return cachedAnalytics;
        }

        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        if(urlMapping != null) {
            List<ClickEventDTO> analytics = clickEventRepo.findByUrlMappingAndClickTimeBetween(urlMapping, start, end)
                    .stream().collect(Collectors.groupingBy(click -> click.getClickTime().toLocalDate(),
                            Collectors.counting())).entrySet().stream().map(entry -> {
                                ClickEventDTO dto = new ClickEventDTO();
                                dto.setClickDate(entry.getKey());
                                dto.setClickCount(entry.getValue());
                                return dto;
                    } ).collect(Collectors.toList());
            analyticsCacheService.putUrlAnalytics(shortUrl, start, end, analytics);
            return analytics;
        }
        return Collections.emptyList();
    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(Users user, LocalDate start, LocalDate end) {
        if (user != null && user.getId() != null) {
            Map<LocalDate, Long> cachedTotalClicks = analyticsCacheService.getTotalClicks(user.getId(), start, end);
            if (cachedTotalClicks != null) {
                return cachedTotalClicks;
            }
        }

        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        Map<LocalDate, Long> totalClicks = clickEventRepo.findByUrlMappingInAndClickTimeBetween(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.groupingBy(click -> click.getClickTime().toLocalDate(),
                        Collectors.counting()));
        if (user != null && user.getId() != null) {
            analyticsCacheService.putTotalClicks(user.getId(), start, end, totalClicks);
        }
        return totalClicks;
    }

    public UrlMapping getOriginalUrl(String shortUrl) {
        UrlMapping cachedUrlMapping = shortUrlCacheService.get(shortUrl);
        if (cachedUrlMapping != null) {
            return cachedUrlMapping;
        }

        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        if (urlMapping != null) {
            shortUrlCacheService.put(urlMapping);
        }
        return urlMapping;
    }

    public void trackRedirect(UrlMapping urlMapping) {
        if (urlMapping == null) {
            return;
        }
        clickAnalyticsDispatcher.dispatchClick(urlMapping);
    }

    @Transactional
    public void deleteUrl(Long id, Users user) {
        UrlMapping urlMapping = urlMappingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Short link not found"));

        if (urlMapping.getUser() == null || user == null || !urlMapping.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only delete your own short links");
        }

        clickEventRepo.deleteByUrlMapping(urlMapping);
        urlMappingRepo.delete(urlMapping);
        shortUrlCacheService.evict(urlMapping.getShortUrl());
        analyticsCacheService.evictForShortUrl(urlMapping.getShortUrl());
        if (urlMapping.getUser() != null && urlMapping.getUser().getId() != null) {
            analyticsCacheService.evictForUser(urlMapping.getUser().getId());
        }
    }

    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiresAt() != null && !urlMapping.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
