package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@ConditionalOnProperty(name = "snapurl.redis.enabled", havingValue = "true")
public class RedisShortUrlCacheService implements ShortUrlCacheService {

    private static final String REDIRECT_CACHE_PREFIX = "snapurl:redirect:";
    private static final String VALUE_SEPARATOR = "|";
    private static final String MISSING_MARKER = "__missing__";

    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;
    private final Duration missingCacheTtl;

    public RedisShortUrlCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${snapurl.redis.redirect-cache-ttl-minutes:60}") long ttlMinutes,
            @Value("${snapurl.redis.redirect-miss-cache-seconds:30}") long missingTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofMinutes(ttlMinutes);
        this.missingCacheTtl = Duration.ofSeconds(missingTtlSeconds);
    }

    @Override
    public ShortUrlCacheLookupResult lookup(String shortUrl) {
        String cachedValue = redisTemplate.opsForValue().get(cacheKey(shortUrl));
        if (cachedValue == null || cachedValue.isBlank()) {
            return ShortUrlCacheLookupResult.miss();
        }

        if (MISSING_MARKER.equals(cachedValue)) {
            return ShortUrlCacheLookupResult.knownMissing();
        }

        try {
            String[] parts = cachedValue.split("\\|", 4);
            if (parts.length < 3) {
                redisTemplate.delete(cacheKey(shortUrl));
                return ShortUrlCacheLookupResult.miss();
            }

            UrlMapping urlMapping = new UrlMapping();
            urlMapping.setId(Long.parseLong(parts[0]));
            urlMapping.setShortUrl(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            urlMapping.setOriginalUrl(URLDecoder.decode(parts[2], StandardCharsets.UTF_8));
            if (parts.length == 4 && !parts[3].isBlank()) {
                urlMapping.setExpiresAt(LocalDateTime.parse(parts[3]));
            }
            return ShortUrlCacheLookupResult.hit(urlMapping);
        } catch (RuntimeException ex) {
            log.warn("Failed to deserialize redirect cache for shortUrl={}", shortUrl, ex);
            redisTemplate.delete(cacheKey(shortUrl));
            return ShortUrlCacheLookupResult.miss();
        }
    }

    @Override
    public void put(UrlMapping urlMapping) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey(urlMapping.getShortUrl()),
                    serialize(urlMapping),
                    cacheTtl
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to write redirect cache for shortUrl={}", urlMapping.getShortUrl(), ex);
        }
    }

    @Override
    public void putMissing(String shortUrl) {
        try {
            redisTemplate.opsForValue().set(cacheKey(shortUrl), MISSING_MARKER, missingCacheTtl);
        } catch (RuntimeException ex) {
            log.warn("Failed to write missing redirect cache for shortUrl={}", shortUrl, ex);
        }
    }

    @Override
    public void evict(String shortUrl) {
        redisTemplate.delete(cacheKey(shortUrl));
    }

    private String cacheKey(String shortUrl) {
        return REDIRECT_CACHE_PREFIX + shortUrl;
    }

    private String serialize(UrlMapping urlMapping) {
        String expiresAt = urlMapping.getExpiresAt() != null ? urlMapping.getExpiresAt().toString() : "";
        return String.join(
                VALUE_SEPARATOR,
                String.valueOf(urlMapping.getId()),
                URLEncoder.encode(urlMapping.getShortUrl(), StandardCharsets.UTF_8),
                URLEncoder.encode(urlMapping.getOriginalUrl(), StandardCharsets.UTF_8),
                expiresAt
        );
    }
}
