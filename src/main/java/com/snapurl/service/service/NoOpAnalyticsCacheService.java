package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnMissingBean(AnalyticsCacheService.class)
public class NoOpAnalyticsCacheService implements AnalyticsCacheService {

    @Override
    public List<ClickEventDTO> getUrlAnalytics(String shortUrl, LocalDateTime start, LocalDateTime end) {
        return null;
    }

    @Override
    public void putUrlAnalytics(String shortUrl, LocalDateTime start, LocalDateTime end, List<ClickEventDTO> analytics) {
    }

    @Override
    public Map<LocalDate, Long> getTotalClicks(Long userId, LocalDate start, LocalDate end) {
        return null;
    }

    @Override
    public void putTotalClicks(Long userId, LocalDate start, LocalDate end, Map<LocalDate, Long> totalClicks) {
    }

    @Override
    public void evictForShortUrl(String shortUrl) {
    }

    @Override
    public void evictForUser(Long userId) {
    }
}
