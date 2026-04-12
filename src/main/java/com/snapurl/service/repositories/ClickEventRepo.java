package com.snapurl.service.repositories;

import com.snapurl.service.models.ClickEvent;
import com.snapurl.service.models.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepo extends JpaRepository<ClickEvent, Long> {

    // Find click events for a specific UrlMapping within a date range
    List<ClickEvent> findByUrlMappingAndClickTimeBetween(UrlMapping mapping, LocalDateTime start, LocalDateTime end);
    // For multiple UrlMappings, we can use the following method:
    List<ClickEvent> findByUrlMappingInAndClickTimeBetween(List<UrlMapping> urlMappings, LocalDateTime start, LocalDateTime end);
    @Modifying
    @Transactional
    void deleteByUrlMapping(UrlMapping urlMapping);

}
