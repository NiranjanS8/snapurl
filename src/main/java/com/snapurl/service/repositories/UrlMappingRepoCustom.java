package com.snapurl.service.repositories;

import com.snapurl.service.dtos.UrlMappingPageDTO;
import com.snapurl.service.models.Users;

import java.time.LocalDateTime;

public interface UrlMappingRepoCustom {
    UrlMappingPageDTO searchUserUrls(
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
    );
}
