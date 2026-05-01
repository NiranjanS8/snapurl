package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventDTO;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlAnalyticsService {

    private final UrlMappingRepo urlMappingRepo;
    private final ClickEventRepo clickEventRepo;
    private final AnalyticsCacheService analyticsCacheService;

    public List<ClickEventDTO> getClickEventByDate(String shortUrl, LocalDateTime start, LocalDateTime end, Users user) {
        List<ClickEventDTO> cachedAnalytics = analyticsCacheService.getUrlAnalytics(shortUrl, start, end);
        if (cachedAnalytics != null) {
            return cachedAnalytics;
        }

        UrlMapping urlMapping = urlMappingRepo.findByShortUrl(shortUrl);
        if (urlMapping != null) {
            if (urlMapping.getUser() == null || user == null || !urlMapping.getUser().getId().equals(user.getId())) {
                throw new IllegalArgumentException("You can only view analytics for your own short links");
            }
            List<ClickEventDTO> analytics = toClickEventDtos(
                    clickEventRepo.countByUrlMappingGroupedByDate(urlMapping, start, end)
            );
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
        if (urlMappings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<LocalDate, Long> totalClicks = toDateCountMap(
                clickEventRepo.countByUrlMappingsGroupedByDate(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
        );
        if (user != null && user.getId() != null) {
            analyticsCacheService.putTotalClicks(user.getId(), start, end, totalClicks);
        }
        return totalClicks;
    }

    private List<ClickEventDTO> toClickEventDtos(List<Object[]> dateCounts) {
        return dateCounts.stream().map(row -> {
            ClickEventDTO dto = new ClickEventDTO();
            dto.setClickDate(toLocalDate(row[0]));
            dto.setClickCount(((Number) row[1]).longValue());
            return dto;
        }).collect(Collectors.toList());
    }

    private Map<LocalDate, Long> toDateCountMap(List<Object[]> dateCounts) {
        return dateCounts.stream().collect(Collectors.toMap(
                row -> toLocalDate(row[0]),
                row -> ((Number) row[1]).longValue(),
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
