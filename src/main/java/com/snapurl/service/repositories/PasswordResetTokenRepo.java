package com.snapurl.service.repositories;

import com.snapurl.service.models.PasswordResetToken;
import com.snapurl.service.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    boolean existsByToken(String token);
    List<PasswordResetToken> findByUserAndUsedFalse(Users user);
    void deleteByExpiresAtBefore(LocalDateTime time);
}
