package com.snapurl.service.controllers;

import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.models.Users;
import com.snapurl.service.service.UrlMappingService;
import com.snapurl.service.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingDTO> createShortUrl(@RequestBody Map<String, String> request,
                                                        Principal principal) {

        String originalUrl = request.get("originalUrl");
        Users user = userService.findByUsername(principal.getName());
        UrlMappingDTO urlMappingDTO = urlMappingService.createShortUrl(originalUrl, user);
        return ResponseEntity.ok(urlMappingDTO);
    }

    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingDTO>> getUserUrls(Principal principal) {
        Users user = userService.findByUsername(principal.getName());
        List<UrlMappingDTO> userUrls = urlMappingService.getUrlsByUser(user);
        return ResponseEntity.ok(userUrls);
    }

}
