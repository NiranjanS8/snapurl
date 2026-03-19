package com.snapurl.service.service;

import com.snapurl.service.models.Users;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "snapurl.mail.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPasswordResetEmailService implements PasswordResetEmailService {
    @Override
    public void sendResetCode(Users user, String resetCode, long expiryMinutes) {
        // Mail delivery is intentionally disabled in this mode.
    }
}
