package com.snapurl.service.controllers;

import com.snapurl.service.dtos.ClickEventDTO;
import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.dtos.UrlMappingPageDTO;
import com.snapurl.service.models.Users;
import com.snapurl.service.service.UrlMappingService;
import com.snapurl.service.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@AllArgsConstructor
public class UrlMappingController {

    private UrlMappingService urlMappingService;
    private UserService userService;

    // {"originalUrl": "https://www.example.com/some/long/url"}
    // https://snapurl.com/v9AuvEYE  -->  https://www.example.com/some/long/url

    @PostMapping("/public/shorten")
    public ResponseEntity<?> createPublicShortUrl(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("originalUrl");
        try {
            UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(originalUrl, null);
            return ResponseEntity.ok(urlMappingDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createShortUrl(@RequestBody Map<String, String> request,
                                                        Principal principal) {

        String originalUrl = request.get("originalUrl");
        Users user = userService.findByEmail(principal.getName());
        try {
            UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(originalUrl, user);
            return ResponseEntity.ok(urlMappingDTO);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
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
                                                                   @RequestParam("endDate") String endDate)
    {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        List<ClickEventDTO> analytics = urlMappingService.getClickEventByDate(shortUrl, start, end);
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
        Map<LocalDate, Long> totalClicks = urlMappingService.getTotalClicksByUserAndDate(user, start, end);
        return ResponseEntity.ok(totalClicks);
    }

}
