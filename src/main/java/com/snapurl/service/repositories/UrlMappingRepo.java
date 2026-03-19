package com.snapurl.service.repositories;

import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepo extends JpaRepository<UrlMapping, Long>, UrlMappingRepoCustom {
    UrlMapping findByShortUrl(String shortUrl);
    boolean existsByShortUrl(String shortUrl);
    Optional<UrlMapping> findByUserAndOriginalUrl(Users user, String originalUrl);
    List<UrlMapping> findByUser(Users user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UrlMapping u set u.clickCount = u.clickCount + 1, u.lastAccessed = :clickedAt where u.id = :id")
    int incrementClickCountAndUpdateLastAccessed(@Param("id") Long id, @Param("clickedAt") LocalDateTime clickedAt);
}
