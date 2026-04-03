package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "snapurl.redis.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpShortUrlCacheService implements ShortUrlCacheService {

    @Override
    public ShortUrlCacheLookupResult lookup(String shortUrl) {
        return ShortUrlCacheLookupResult.miss();
    }

    @Override
    public void put(UrlMapping urlMapping) {
    }

    @Override
    public void putMissing(String shortUrl) {
    }

    @Override
    public void evict(String shortUrl) {
    }
}
