package com.snapurl.service.service;

import com.snapurl.service.models.Users;

public interface PasswordResetEmailService {
    void sendResetCode(Users user, String resetCode, long expiryMinutes);
}
