package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;

public interface ShortUrlCacheService {
    UrlMapping get(String shortUrl);
    void put(UrlMapping urlMapping);
    void evict(String shortUrl);
}
