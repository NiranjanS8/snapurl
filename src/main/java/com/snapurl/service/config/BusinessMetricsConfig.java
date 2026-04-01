package com.snapurl.service.config;

import com.snapurl.service.repositories.ClickEventRepo;
import com.snapurl.service.repositories.RefreshTokenRepo;
import com.snapurl.service.repositories.UrlMappingRepo;
import com.snapurl.service.repositories.UserRepo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessMetricsConfig {

    @Bean
    public Object snapUrlBusinessMetrics(
            MeterRegistry meterRegistry,
            UserRepo userRepo,
            UrlMappingRepo urlMappingRepo,
            ClickEventRepo clickEventRepo,
            RefreshTokenRepo refreshTokenRepo
    ) {
        Gauge.builder("snapurl.users.total", userRepo, UserRepo::count)
                .description("Total registered users")
                .register(meterRegistry);

        Gauge.builder("snapurl.links.total", urlMappingRepo, UrlMappingRepo::count)
                .description("Total short links")
                .register(meterRegistry);

        Gauge.builder("snapurl.clicks.total", clickEventRepo, ClickEventRepo::count)
                .description("Total click events processed")
                .register(meterRegistry);

        Gauge.builder("snapurl.refresh.tokens.active", refreshTokenRepo, RefreshTokenRepo::countByRevokedFalse)
                .description("Active refresh tokens")
                .register(meterRegistry);

        return new Object();
    }
}
