package com.snapurl.service.config;

import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.RefreshTokenRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import com.snapurl.service.repositories.UserRepo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@Slf4j
public class BusinessMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final UserRepo userRepo;
    private final UrlMappingRepo urlMappingRepo;
    private final ClickEventRepo clickEventRepo;
    private final RefreshTokenRepo refreshTokenRepo;

    private final AtomicLong cachedUserCount = new AtomicLong(0);
    private final AtomicLong cachedLinkCount = new AtomicLong(0);
    private final AtomicLong cachedClickCount = new AtomicLong(0);
    private final AtomicLong cachedActiveRefreshTokens = new AtomicLong(0);

    public BusinessMetricsConfig(
            MeterRegistry meterRegistry,
            UserRepo userRepo,
            UrlMappingRepo urlMappingRepo,
            ClickEventRepo clickEventRepo,
            RefreshTokenRepo refreshTokenRepo
    ) {
        this.meterRegistry = meterRegistry;
        this.userRepo = userRepo;
        this.urlMappingRepo = urlMappingRepo;
        this.clickEventRepo = clickEventRepo;
        this.refreshTokenRepo = refreshTokenRepo;
    }

    @PostConstruct
    void registerGauges() {
        Gauge.builder("snapurl.users.total", cachedUserCount, AtomicLong::doubleValue)
                .description("Total registered users")
                .register(meterRegistry);

        Gauge.builder("snapurl.links.total", cachedLinkCount, AtomicLong::doubleValue)
                .description("Total short links")
                .register(meterRegistry);

        Gauge.builder("snapurl.clicks.total", cachedClickCount, AtomicLong::doubleValue)
                .description("Total click events processed")
                .register(meterRegistry);

        Gauge.builder("snapurl.refresh.tokens.active", cachedActiveRefreshTokens, AtomicLong::doubleValue)
                .description("Active refresh tokens")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${snapurl.metrics.refresh-interval-ms:60000}")
    void refreshCounts() {
        try {
            cachedUserCount.set(userRepo.count());
            cachedLinkCount.set(urlMappingRepo.count());
            cachedClickCount.set(clickEventRepo.count());
            cachedActiveRefreshTokens.set(refreshTokenRepo.countByRevokedFalse());
        } catch (RuntimeException ex) {
            log.warn("Failed to refresh business metric counts", ex);
        }
    }
}
