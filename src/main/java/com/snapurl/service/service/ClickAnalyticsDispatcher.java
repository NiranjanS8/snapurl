package com.snapurl.service.service;

import com.snapurl.service.models.UrlMapping;

public interface ClickAnalyticsDispatcher {
    void dispatchClick(UrlMapping urlMapping);
}
