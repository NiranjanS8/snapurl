package com.snapurl.service.repositories;

import com.snapurl.service.models.RefreshToken;
import com.snapurl.service.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevokedFalse(Users user);
    long countByRevokedFalse();
    void deleteByUser(Users user);
    void deleteByExpiresAtBefore(LocalDateTime time);
}
