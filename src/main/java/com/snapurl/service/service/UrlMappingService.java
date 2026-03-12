package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventDTO;
import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UrlMappingService {

    private UrlMappingRepo urlMappingRepo;
    private ClickEventRepo clickEventRepo;

    public UrlMappingDTO createShortUrl(String originalUrl, Users user) {

        String shortUrl = generateShortUrl(originalUrl);
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setShortUrl(shortUrl);
        urlMapping.setUser(user);
        urlMapping.setCreatedAt(LocalDate.now());

        UrlMapping savedMapping = urlMappingRepo.save(urlMapping);
        return convertToDTO(savedMapping);

    }

    private UrlMappingDTO convertToDTO(UrlMapping urlMapping) {
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setId(urlMapping.getId());
        dto.setOriginalUrl(urlMapping.getOriginalUrl());
        dto.setShortUrl(urlMapping.getShortUrl());
        dto.setClickCount(urlMapping.getClickCount());
        dto.setCreatedAt(urlMapping.getCreatedAt().atStartOfDay());
        dto.setUsername(urlMapping.getUser().getUsername());
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

    public List<UrlMappingDTO> getUrlsByUser(Users user) {
        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        return urlMappings.stream().map(this::convertToDTO).toList();
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
        return null;
    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(Users user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepo.findByUser(user);
        return clickEventRepo.findByUrlMappingInAndClickTimeBetween(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream().collect(Collectors.groupingBy(click -> click.getClickTime().toLocalDate(),
                        Collectors.counting()));
    }
}
