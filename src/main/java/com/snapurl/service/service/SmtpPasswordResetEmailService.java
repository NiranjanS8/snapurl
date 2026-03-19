package com.snapurl.service.service;

import com.snapurl.service.models.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "snapurl.mail.enabled", havingValue = "true")
public class SmtpPasswordResetEmailService implements PasswordResetEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpPasswordResetEmailService(
            JavaMailSender mailSender,
            @Value("${snapurl.mail.from}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendResetCode(Users user, String resetCode, long expiryMinutes) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("Password reset email is enabled, but snapurl.mail.from is not configured.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Your SnapURL password reset code");
        message.setText(buildMessage(user.getUsername(), resetCode, expiryMinutes));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new IllegalStateException("Password reset email could not be sent. Check your SMTP settings.", ex);
        }
    }

    private String buildMessage(String username, String resetCode, long expiryMinutes) {
        return String.join(
                System.lineSeparator(),
                "Hi " + username + ",",
                "",
                "We received a request to reset your SnapURL password.",
                "",
                "Your one-time reset code is: " + resetCode,
                "",
                "This code will expire in " + expiryMinutes + " minutes.",
                "If you didn't request this, you can safely ignore this email.",
                "",
                "SnapURL"
        );
    }
}
