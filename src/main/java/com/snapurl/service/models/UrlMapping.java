package com.snapurl.service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_url_mapping_user_created", columnList = "user_id, createdAt"),
                @Index(name = "idx_url_mapping_user_clicks", columnList = "user_id, clickCount"),
                @Index(name = "idx_url_mapping_user_last_accessed", columnList = "user_id, lastAccessed"),
                @Index(name = "idx_url_mapping_user_expires", columnList = "user_id, expiresAt"),
                @Index(name = "idx_url_mapping_short_url", columnList = "shortUrl"),
                @Index(name = "idx_url_mapping_original_url", columnList = "originalUrl")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private int clickCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;
    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private  Users user;

    @OneToMany(mappedBy = "urlMapping")
    private List<ClickEvent> clickEvents;
}
