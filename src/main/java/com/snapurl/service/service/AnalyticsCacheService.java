package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AnalyticsCacheService {
    List<ClickEventDTO> getUrlAnalytics(String shortUrl, LocalDateTime start, LocalDateTime end);
    void putUrlAnalytics(String shortUrl, LocalDateTime start, LocalDateTime end, List<ClickEventDTO> analytics);
    Map<LocalDate, Long> getTotalClicks(Long userId, LocalDate start, LocalDate end);
    void putTotalClicks(Long userId, LocalDate start, LocalDate end, Map<LocalDate, Long> totalClicks);
    void evictForShortUrl(String shortUrl);
    void evictForUser(Long userId);
}
