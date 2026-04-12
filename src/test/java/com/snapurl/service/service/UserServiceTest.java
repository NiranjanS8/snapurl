package com.snapurl.service.service;

import com.snapurl.service.dtos.LoginRequest;
import com.snapurl.service.dtos.RefreshTokenRequest;
import com.snapurl.service.dtos.ResetPasswordRequest;
import com.snapurl.service.models.PasswordResetToken;
import com.snapurl.service.models.RefreshToken;
import com.snapurl.service.models.Users;
import com.snapurl.service.repositories.PasswordResetTokenRepo;
import com.snapurl.service.repositories.RefreshTokenRepo;
import com.snapurl.service.repositories.UserRepo;
import com.snapurl.service.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepo userRepo;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AppMetricsService appMetricsService;
    @Mock
    private RefreshTokenRepo refreshTokenRepo;
    @Mock
    private PasswordResetTokenRepo passwordResetTokenRepo;
    @Mock
    private PasswordResetEmailService passwordResetEmailService;

    @InjectMocks
    private UserService userService;

    private Users user;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setId(1L);
        user.setEmail("user@gmail.com");
        user.setUsername("tester");
        user.setPassword("password123");
        user.setRole("ROLE_USER");

        ReflectionTestUtils.setField(userService, "refreshTokenExpirationMs", 604800000L);
        ReflectionTestUtils.setField(userService, "passwordResetExpirationMinutes", 30L);
        ReflectionTestUtils.setField(userService, "exposeResetToken", true);
        ReflectionTestUtils.setField(userService, "maxFailedLoginAttempts", 3);
        ReflectionTestUtils.setField(userService, "accountLockMinutes", 15L);
    }

    @Test
    void registerUserRejectsUnsupportedEmailDomain() {
        user.setEmail("user@a.com");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(user)
        );

        assertEquals("Please use a supported email provider like Gmail, Proton Mail, iCloud Mail, or Outlook.", exception.getMessage());
    }

    @Test
    void refreshAccessTokenRotatesRefreshToken() {
        RefreshToken existingRefreshToken = new RefreshToken();
        existingRefreshToken.setId(100L);
        existingRefreshToken.setToken("old-refresh");
        existingRefreshToken.setUser(user);
        existingRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        existingRefreshToken.setRevoked(false);

        RefreshToken rotatedToken = new RefreshToken();
        rotatedToken.setToken("new-refresh");
        rotatedToken.setUser(user);
        rotatedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        rotatedToken.setRevoked(false);

        when(refreshTokenRepo.findByToken("old-refresh")).thenReturn(Optional.of(existingRefreshToken));
        when(jwtUtils.generateToken(user.getEmail(), user.getRole())).thenReturn("new-access");
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() != null && token.getId().equals(100L)) {
                return token;
            }
            rotatedToken.setToken(token.getToken());
            rotatedToken.setExpiresAt(token.getExpiresAt());
            rotatedToken.setUser(token.getUser());
            return rotatedToken;
        });

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("old-refresh");

        var response = userService.refreshAccessToken(refreshTokenRequest);

        assertEquals("new-access", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertNotEquals("old-refresh", response.getRefreshToken());
        verify(refreshTokenRepo).findByToken("old-refresh");
        verify(refreshTokenRepo, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    void loginUserReturnsAccessAndRefreshTokens() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserDetailsImpl.build(user),
                null,
                UserDetailsImpl.build(user).getAuthorities()
        );
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("password123");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepo.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("access-token");
        when(refreshTokenRepo.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.loginUser(loginRequest);

        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void loginUserLocksAccountAfterConfiguredFailedAttempts() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("wrong-password");
        user.setFailedLoginAttempts(2);

        when(userRepo.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> userService.loginUser(loginRequest));

        assertEquals(0, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        verify(userRepo).save(user);
    }

    @Test
    void loginUserRejectsAlreadyLockedAccount() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("password123");
        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));

        when(userRepo.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));

        AccountLockedException exception = assertThrows(
                AccountLockedException.class,
                () -> userService.loginUser(loginRequest)
        );

        assertEquals("Too many failed login attempts. Your account is temporarily locked. Please try again later.", exception.getMessage());
    }

    @Test
    void requestPasswordResetReturnsTokenForExistingSupportedUser() {
        when(userRepo.findByEmail("user@gmail.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepo.existsByToken(any())).thenReturn(false);
        when(passwordResetTokenRepo.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.requestPasswordReset("user@gmail.com");

        assertEquals("If an account exists for that email, a reset code has been prepared.", response.getMessage());
        assertNotNull(response.getResetCode());
        verify(passwordResetEmailService).sendResetCode(eq(user), any(), eq(30L));
    }

    @Test
    void resetPasswordUpdatesPasswordAndRevokesActiveRefreshTokens() {
        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken("reset-token");
        passwordResetToken.setUser(user);
        passwordResetToken.setUsed(false);
        passwordResetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        RefreshToken activeRefreshToken = new RefreshToken();
        activeRefreshToken.setToken("refresh-token");
        activeRefreshToken.setUser(user);
        activeRefreshToken.setRevoked(false);
        activeRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setCode("reset-token");
        request.setPassword("newPassword123");

        when(passwordResetTokenRepo.findByToken("reset-token")).thenReturn(Optional.of(passwordResetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-password");
        when(refreshTokenRepo.findByUserAndRevokedFalse(user)).thenReturn(List.of(activeRefreshToken));

        userService.resetPassword(request);

        assertEquals("encoded-password", user.getPassword());
        assertEquals(true, passwordResetToken.isUsed());
        assertEquals(true, activeRefreshToken.isRevoked());
        verify(userRepo).save(user);
        verify(passwordResetTokenRepo).save(passwordResetToken);
        verify(refreshTokenRepo).save(activeRefreshToken);
    }
}
