package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "snapurl.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpShortUrlCacheService implements ShortUrlCacheService {

    @Override
    public UrlMapping get(String shortUrl) {
        return null;
    }

    @Override
    public void put(UrlMapping urlMapping) {
    }

    @Override
    public void evict(String shortUrl) {
    }
}
