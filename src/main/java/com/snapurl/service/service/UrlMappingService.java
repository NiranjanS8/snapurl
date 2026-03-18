package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventDTO;
import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.dtos.UrlMappingPageDTO;
import com.snapurl.service.models.ClickEvent;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {

    public static final String INVALID_URL_MESSAGE = "We'll need a valid URL, like \"super-long-link.com/shorten-it\"";

    private UrlMappingRepo urlMappingRepo;
    private ClickEventRepo clickEventRepo;

    public UrlMappingDTO createShortUrl(String originalUrl, Users user) {
        if (!isValidUrl(originalUrl)) {
            throw new IllegalArgumentException(INVALID_URL_MESSAGE);
        }

        String shortUrl = generateShortUrl(originalUrl);
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedAt(LocalDateTime.now());
        urlMapping.setExpiresAt(LocalDateTime.now().plusYears(1));

        UrlMapping savedMapping = urlMappingRepo.save(urlMapping);
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
    private String generateShortUrl(String originalUrl) {
        Random random = new Random();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder shortUrl = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            shortUrl.append(characters.charAt(random.nextInt(characters.length())));
        }
        return shortUrl.toString();
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
            return host != null && !host.isBlank() && host.contains(".");
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
        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        if(urlMapping != null) {
            return clickEventRepo.findByUrlMappingAndClickTimeBetween(urlMapping, start, end)
                    .stream().collect(Collectors.groupingBy(click -> click.getClickTime().toLocalDate(),
                            Collectors.counting())).entrySet().stream().map(entry -> {
                                ClickEventDTO dto = new ClickEventDTO();
                                dto.setClickDate(entry.getKey());
                                dto.setClickCount(entry.getValue());
                                return dto;
                    } ).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(Users user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        return clickEventRepo.findByUrlMappingInAndClickTimeBetween(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.groupingBy(click -> click.getClickTime().toLocalDate(),
                        Collectors.counting()));
    }

    public UrlMapping getOriginalUrl(String shortUrl) {

        UrlMapping  urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        if(urlMapping != null){
            urlMapping.setClickCount(urlMapping.getClickCount() + 1);
            urlMapping.setLastAccessed(LocalDateTime.now());
            urlMappingRepo.save(urlMapping);

            ClickEvent clickEvent = new ClickEvent();
            clickEvent.setUrlMapping(urlMapping);
            clickEvent.setClickTime(LocalDateTime.now());
            clickEventRepo.save(clickEvent);
        }
        return urlMapping;
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
    }

    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiresAt() != null && !urlMapping.getExpiresAt().isAfter(LocalDateTime.now());
    }
}
