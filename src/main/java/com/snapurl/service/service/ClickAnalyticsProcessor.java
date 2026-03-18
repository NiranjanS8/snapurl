package com.snapurl.service.service;

import com.snapurl.service.dtos.ClickEventMessage;
import com.snapurl.service.models.ClickEvent;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ClickAnalyticsProcessor {

    private final UrlMappingRepo urlMappingRepo;
    private final ClickEventRepo clickEventRepo;

    @Transactional
    public void processClick(ClickEventMessage message) {
        UrlMapping urlMapping = urlMappingRepo.findById(message.getUrlMappingId())
                .orElse(null);

        if (urlMapping == null) {
            return;
        }

        urlMapping.setClickCount(urlMapping.getClickCount() + 1);
        urlMapping.setLastAccessed(message.getClickedAt());
        urlMappingRepo.save(urlMapping);

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setUrlMapping(urlMapping);
        clickEvent.setClickTime(message.getClickedAt());
        clickEventRepo.save(clickEvent);
    }
}
