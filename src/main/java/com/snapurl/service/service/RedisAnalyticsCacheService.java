package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@ConditionalOnProperty(name = "snapurl.redis.enabled", havingValue = "true")
public class RedisAnalyticsCacheService implements AnalyticsCacheService {

    private static final String URL_ANALYTICS_PREFIX = "snapurl:analytics:url:";
    private static final String USER_TOTAL_CLICKS_PREFIX = "snapurl:analytics:user:";

    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    public RedisAnalyticsCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${snapurl.redis.analytics-cache-ttl-seconds:60}") long ttlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public List<ClickEventDTO> getUrlAnalytics(String shortUrl, LocalDateTime start, LocalDateTime end) {
        String raw = redisTemplate.opsForValue().get(urlAnalyticsKey(shortUrl, start, end));
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            List<ClickEventDTO> items = new ArrayList<>();
            for (String entry : raw.split(";")) {
                if (entry.isBlank()) {
                    continue;
                }
                String[] parts = entry.split(",", 2);
                if (parts.length != 2) {
                    continue;
                }
                ClickEventDTO dto = new ClickEventDTO();
                dto.setClickDate(LocalDate.parse(parts[0]));
                dto.setClickCount(Long.parseLong(parts[1]));
                items.add(dto);
            }
            return items;
        } catch (RuntimeException ex) {
            log.warn("Failed to deserialize cached url analytics for shortUrl={}", shortUrl, ex);
            redisTemplate.delete(urlAnalyticsKey(shortUrl, start, end));
            return null;
        }
    }

    @Override
    public void putUrlAnalytics(String shortUrl, LocalDateTime start, LocalDateTime end, List<ClickEventDTO> analytics) {
        try {
            StringBuilder value = new StringBuilder();
            for (ClickEventDTO item : analytics) {
                if (!value.isEmpty()) {
                    value.append(';');
                }
                value.append(item.getClickDate()).append(',').append(item.getClickCount());
            }
            redisTemplate.opsForValue().set(urlAnalyticsKey(shortUrl, start, end), value.toString(), cacheTtl);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache url analytics for shortUrl={}", shortUrl, ex);
        }
    }

    @Override
    public Map<LocalDate, Long> getTotalClicks(Long userId, LocalDate start, LocalDate end) {
        String raw = redisTemplate.opsForValue().get(totalClicksKey(userId, start, end));
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            Map<LocalDate, Long> result = new LinkedHashMap<>();
            for (String entry : raw.split(";")) {
                if (entry.isBlank()) {
                    continue;
                }
                String[] parts = entry.split(",", 2);
                if (parts.length != 2) {
                    continue;
                }
                result.put(LocalDate.parse(parts[0]), Long.parseLong(parts[1]));
            }
            return result;
        } catch (RuntimeException ex) {
            log.warn("Failed to deserialize cached total clicks for userId={}", userId, ex);
            redisTemplate.delete(totalClicksKey(userId, start, end));
            return null;
        }
    }

    @Override
    public void putTotalClicks(Long userId, LocalDate start, LocalDate end, Map<LocalDate, Long> totalClicks) {
        try {
            StringBuilder value = new StringBuilder();
            for (Map.Entry<LocalDate, Long> entry : totalClicks.entrySet()) {
                if (!value.isEmpty()) {
                    value.append(';');
                }
                value.append(entry.getKey()).append(',').append(entry.getValue());
            }
            redisTemplate.opsForValue().set(totalClicksKey(userId, start, end), value.toString(), cacheTtl);
        } catch (RuntimeException ex) {
            log.warn("Failed to cache total clicks for userId={}", userId, ex);
        }
    }

    @Override
    public void evictForShortUrl(String shortUrl) {
        deleteByPattern(URL_ANALYTICS_PREFIX + shortUrl + ":*");
    }

    @Override
    public void evictForUser(Long userId) {
        deleteByPattern(USER_TOTAL_CLICKS_PREFIX + userId + ":*");
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String urlAnalyticsKey(String shortUrl, LocalDateTime start, LocalDateTime end) {
        return URL_ANALYTICS_PREFIX + shortUrl + ":" + start + ":" + end;
    }

    private String totalClicksKey(Long userId, LocalDate start, LocalDate end) {
        return USER_TOTAL_CLICKS_PREFIX + userId + ":" + start + ":" + end;
    }
}
