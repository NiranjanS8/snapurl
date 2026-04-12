package com.snapurl.service.config;

import com.snapurl.service.repositories.PasswordResetTokenRepo;
import com.snapurl.service.repositories.RefreshTokenRepo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
@Slf4j
public class ExpiredTokenCleanupTask {

    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordResetTokenRepo passwordResetTokenRepo;

    @Scheduled(fixedDelayString = "${snapurl.cleanup.interval-ms:3600000}")
    void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        try {
            refreshTokenRepo.deleteByExpiresAtBefore(now);
            passwordResetTokenRepo.deleteByExpiresAtBefore(now);
            log.info("Expired token cleanup completed");
        } catch (RuntimeException ex) {
            log.warn("Expired token cleanup failed", ex);
        }
    }
}
