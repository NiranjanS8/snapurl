package com.snapurl.service.service;

import com.snapurl.service.dtos.ForgotPasswordResponse;
import com.snapurl.service.dtos.LoginRequest;
import com.snapurl.service.dtos.RefreshTokenRequest;
import com.snapurl.service.dtos.ResetPasswordRequest;
import com.snapurl.service.models.PasswordResetToken;
import com.snapurl.service.models.RefreshToken;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.PasswordResetTokenRepo;
import com.snapurl.service.repositories.RefreshTokenRepo;
import com.snapurl.service.repositories.UserRepo;
import com.snapurl.service.security.JwtAuthenticationResponse;
import com.snapurl.service.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "gmail.com",
            "googlemail.com",
            "proton.me",
            "protonmail.com",
            "icloud.com",
            "me.com",
            "mac.com",
            "outlook.com",
            "hotmail.com",
            "live.com"
    );
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,24}$";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordEncoder passwordEncoder;
    private UserRepo userRepo;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private RefreshTokenRepo refreshTokenRepo;
    private PasswordResetTokenRepo passwordResetTokenRepo;
    private PasswordResetEmailService passwordResetEmailService;

    public UserService(
            PasswordEncoder passwordEncoder,
            UserRepo userRepo,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            RefreshTokenRepo refreshTokenRepo,
            PasswordResetTokenRepo passwordResetTokenRepo,
            PasswordResetEmailService passwordResetEmailService
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordResetTokenRepo = passwordResetTokenRepo;
        this.passwordResetEmailService = passwordResetEmailService;
    }

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;
    @Value("${snapurl.auth.password-reset-expiration-minutes:30}")
    private long passwordResetExpirationMinutes;
    @Value("${snapurl.auth.expose-reset-token:true}")
    private boolean exposeResetToken;


    // This method handles user registration by encoding the password and saving the user to the database
    @Transactional
    public Users registerUser(Users user) {
        validateEmail(user.getEmail());
        validateUsername(user.getUsername());
        validatePassword(user.getPassword());

        if (userRepo.findByEmail(user.getEmail().trim().toLowerCase()).isPresent()) {
            throw new IllegalStateException("An account with this email already exists.");
        }
        if (userRepo.findByUsername(user.getUsername().trim()).isPresent()) {
            throw new IllegalStateException("That username is already taken.");
        }

        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    // This method handles user login and returns a JWT token if authentication is successful
    @Transactional
    public JwtAuthenticationResponse loginUser(LoginRequest loginRequest) {
        validateEmail(loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail().trim().toLowerCase(), loginRequest.getPassword())
        );
        // If authentication is successful, set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String accessToken = jwtUtils.generateToken(userDetails);
        Users user = findByEmail(userDetails.getEmail());
        String refreshToken = createRefreshToken(user).getToken();

        return new JwtAuthenticationResponse(accessToken, refreshToken, "Bearer");
    }

    public Users findByUsername(String name) {
        return userRepo.findByUsername(name).orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + name)
        );
    }

    public Users findByEmail(String email) {
        return userRepo.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + email)
        );
    }

    @Transactional
    public JwtAuthenticationResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshTokenValue = refreshTokenRequest.getRefreshToken();
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required.");
        }

        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token has expired or was revoked.");
        }

        Users user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepo.save(refreshToken);

        String accessToken = jwtUtils.generateToken(user.getEmail(), user.getRole());
        String rotatedRefreshToken = createRefreshToken(user).getToken();

        return new JwtAuthenticationResponse(accessToken, rotatedRefreshToken, "Bearer");
    }

    @Transactional
    public ForgotPasswordResponse requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!isAllowedEmail(normalizedEmail)) {
            return new ForgotPasswordResponse("If an account exists for that email, a reset code has been prepared.", null);
        }

        Users user = userRepo.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return new ForgotPasswordResponse("If an account exists for that email, a reset code has been prepared.", null);
        }

        passwordResetTokenRepo.deleteByExpiresAtBefore(LocalDateTime.now());
        passwordResetTokenRepo.findByUserAndUsedFalse(user).forEach(token -> {
            token.setUsed(true);
            passwordResetTokenRepo.save(token);
        });

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(generateResetCode());
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiresAt(LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes));
        passwordResetTokenRepo.save(passwordResetToken);
        passwordResetEmailService.sendResetCode(user, passwordResetToken.getToken(), passwordResetExpirationMinutes);

        return new ForgotPasswordResponse(
                "If an account exists for that email, a reset code has been prepared.",
                exposeResetToken ? passwordResetToken.getToken() : null
        );
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        if (resetPasswordRequest.getCode() == null || resetPasswordRequest.getCode().isBlank()) {
            throw new IllegalArgumentException("Reset code is required.");
        }
        validatePassword(resetPasswordRequest.getPassword());

        PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(resetPasswordRequest.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code."));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        Users user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(resetPasswordRequest.getPassword()));
        userRepo.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepo.save(resetToken);

        refreshTokenRepo.findByUserAndRevokedFalse(user).forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepo.save(token);
        });
    }

    private RefreshToken createRefreshToken(Users user) {
        refreshTokenRepo.deleteByExpiresAtBefore(LocalDateTime.now());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString().replace("-", ""));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs)));
        refreshToken.setRevoked(false);
        return refreshTokenRepo.save(refreshToken);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!isAllowedEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Please use a supported email provider like Gmail, Proton Mail, iCloud Mail, or Outlook.");
        }
    }

    private boolean isAllowedEmail(String email) {
        if (!email.matches(EMAIL_PATTERN)) {
            return false;
        }

        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            return false;
        }

        String domain = email.substring(atIndex + 1);
        return ALLOWED_EMAIL_DOMAINS.contains(domain);
    }

    private String generateResetCode() {
        String code;
        do {
            code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        } while (passwordResetTokenRepo.existsByToken(code));
        return code;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long.");
        }
    }
}
