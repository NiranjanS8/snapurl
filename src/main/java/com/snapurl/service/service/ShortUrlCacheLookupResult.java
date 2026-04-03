package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;

public class ShortUrlCacheLookupResult {

    public enum Status {
        HIT,
        MISS,
        KNOWN_MISSING
    }

    private static final ShortUrlCacheLookupResult MISS_RESULT = new ShortUrlCacheLookupResult(Status.MISS, null);
    private static final ShortUrlCacheLookupResult KNOWN_MISSING_RESULT = new ShortUrlCacheLookupResult(Status.KNOWN_MISSING, null);

    private final Status status;
    private final UrlMapping urlMapping;

    private ShortUrlCacheLookupResult(Status status, UrlMapping urlMapping) {
        this.status = status;
        this.urlMapping = urlMapping;
    }

    public static ShortUrlCacheLookupResult hit(UrlMapping urlMapping) {
        return new ShortUrlCacheLookupResult(Status.HIT, urlMapping);
    }

    public static ShortUrlCacheLookupResult miss() {
        return MISS_RESULT;
    }

    public static ShortUrlCacheLookupResult knownMissing() {
        return KNOWN_MISSING_RESULT;
    }

    public Status getStatus() {
        return status;
    }

    public UrlMapping getUrlMapping() {
        return urlMapping;
    }

    public boolean isHit() {
        return status == Status.HIT;
    }

    public boolean isKnownMissing() {
        return status == Status.KNOWN_MISSING;
    }
}
