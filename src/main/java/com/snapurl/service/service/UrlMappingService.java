package com.snapurl.service.service;

import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@AllArgsConstructor
public class UrlMappingService {

    private UrlMappingRepo urlMappingRepo;

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
}
