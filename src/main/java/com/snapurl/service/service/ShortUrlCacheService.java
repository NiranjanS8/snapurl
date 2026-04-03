package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;

public interface ShortUrlCacheService {
    ShortUrlCacheLookupResult lookup(String shortUrl);
    void put(UrlMapping urlMapping);
    void putMissing(String shortUrl);
    void evict(String shortUrl);
}
