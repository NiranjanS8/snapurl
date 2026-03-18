package com.snapurl.service.repositories;

import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepo extends JpaRepository<UrlMapping, Long>, UrlMappingRepoCustom {
    UrlMapping findByShortUrl(String shortUrl);
    boolean existsByShortUrl(String shortUrl);
    Optional<UrlMapping> findByUserAndOriginalUrl(Users user, String originalUrl);
    List<UrlMapping> findByUser(Users user);
}
